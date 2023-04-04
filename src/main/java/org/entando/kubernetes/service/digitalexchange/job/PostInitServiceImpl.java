package org.entando.kubernetes.service.digitalexchange.job;

import static org.awaitility.Awaitility.await;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallAction;
import org.entando.kubernetes.exception.EntandoGeneralException;
import org.entando.kubernetes.exception.digitalexchange.InvalidBundleException;
import org.entando.kubernetes.exception.k8ssvc.BundleNotFoundException;
import org.entando.kubernetes.model.bundle.BundleInfo;
import org.entando.kubernetes.model.bundle.EntandoBundle;
import org.entando.kubernetes.model.common.EntandoDeploymentPhase;
import org.entando.kubernetes.model.debundle.EntandoDeBundle;
import org.entando.kubernetes.model.debundle.EntandoDeBundleTag;
import org.entando.kubernetes.model.job.EntandoBundleJobEntity;
import org.entando.kubernetes.model.job.JobStatus;
import org.entando.kubernetes.service.KubernetesService;
import org.entando.kubernetes.service.digitalexchange.BundleUtilities;
import org.entando.kubernetes.service.digitalexchange.component.EntandoBundleService;
import org.entando.kubernetes.validator.ValidationFunctions;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PostInitServiceImpl implements PostInitService, InitializingBean {

    public static final String ACTION_INSTALL_OR_UPDATE = "install-or-update";
    public static final String DEFAULT_ACTION = "deploy-only";
    public static final String ECR_ACTION_UNINSTALL = "uninstall";
    private final EntandoBundleService bundleService;
    private final EntandoBundleInstallService installService;
    private final EntandoBundleJobService entandoBundleJobService;
    private final KubernetesService kubernetesService;
    private final String postInitConfigurationData;
    private static final ObjectMapper mapper = new ObjectMapper();

    private PostInitStatus status;
    private boolean finished;
    private static final int MAX_RETIES = 5000;
    private static final int DEFAULT_CONFIGURATION_TIMEOUT = 600;
    private static final int DEFAULT_CONFIGURATION_FREQUENCY = 5;
    private int retries = 0;
    private PostInitData configurationData;
    private static final PostInitData DEFAULT_CONFIGURATION_DATA;
    private static final Duration MAX_WAITING_TIME_FOR_JOB_STATUS = Duration.ofSeconds(360);
    private static final Duration AWAITILY_DEFAULT_POLL_INTERVAL = Duration.ofSeconds(6);


    static {
        List<PostInitItem> items = new ArrayList<>();
        items.add(PostInitItem.builder()
                .name("entando-epc-bootstrap")
                .url("docker://registry.hub.docker.com/entando/entando-bootstrap-bundle")
                .version("1.0.1")
                .action(ACTION_INSTALL_OR_UPDATE)
                .priority(1)
                .build());
        DEFAULT_CONFIGURATION_DATA = PostInitData.builder()
                .frequency(DEFAULT_CONFIGURATION_FREQUENCY)
                .maxAppWait(DEFAULT_CONFIGURATION_TIMEOUT)
                .items(items).build();

    }

    public PostInitServiceImpl(@Value("${entando.ecr.postinit:#{null}}") String postInitConfigurationData,
            EntandoBundleService bundleService, EntandoBundleInstallService installService,
            KubernetesService kubernetesService, EntandoBundleJobService entandoBundleJobService) {
        this.postInitConfigurationData = postInitConfigurationData;
        this.bundleService = bundleService;
        this.installService = installService;
        this.kubernetesService = kubernetesService;
        this.entandoBundleJobService = entandoBundleJobService;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        configurationData = parsePostInitConfiguration().orElse(DEFAULT_CONFIGURATION_DATA);
        status = PostInitStatus.UNKNOWN;
        finished = false;
    }

    @Override
    public boolean shouldRetry() {
        return retries < MAX_RETIES;
    }

    @Override
    public int getFrequencyInSeconds() {
        return configurationData.getFrequency();
    }

    @Override
    public int getMaxAppWaitInSeconds() {
        return configurationData.getMaxAppWait();
    }

    @Override
    public Optional<Boolean> isEcrActionAllowed(String bundleCode, String action) {
        return configurationData.getItems().stream()
                .filter(item -> StringUtils.equals(calculateBundleCode(item), bundleCode))
                .findFirst()
                .map(item -> Boolean.valueOf(ecrActionAllowed(item, action)))
                .or(Optional::empty);
    }

    @Override
    public void install() {
        log.debug("Post init install started");

        finished = false;
        status = PostInitStatus.STARTED;
        retries++;

        Optional<String> appStatus = retrieveApplicationStatus();
        if (appStatus.isPresent() && EntandoDeploymentPhase.SUCCESSFUL.toValue().toLowerCase()
                .equals(appStatus.get())) {
            log.info("Application is ready, starting the post-init installation process");
            // sort bundle ootb to manage priority
            Comparator<PostInitItem> compareByPriorityAndThenName = Comparator
                    .comparingInt(PostInitItem::getPriority).reversed()
                    .thenComparing(PostInitItem::getName);

            List<PostInitItem> bundleToInstall = configurationData.getItems().stream()
                    .sorted(compareByPriorityAndThenName)
                    .collect(Collectors.toList());

            // list ALL bundles installed (for update) or not
            Map<String, EntandoBundle> bundlesInstalledOrDeployed = bundleService.listBundles().getBody().stream()
                    .collect(Collectors.toMap(
                            EntandoBundle::getCode,
                            Function.identity(),
                            (item1, item2) -> item1));

            try {

                for (PostInitItem itemFromConfig : bundleToInstall) {
                    final PostInitItem item = checkActionOrSwitchToDefault(itemFromConfig);
                    log.info("Post init installing action '{}' on bundle '{}'", item.getAction(), item.getName());

                    final String bundleCode = calculateBundleCode(item);

                    EntandoBundle bundle = Optional.ofNullable(bundlesInstalledOrDeployed.get(bundleCode))
                            .orElseGet(() -> deployPostInitBundle(item));

                    computeInstallStrategy(bundle, item)
                            .or(() -> computeUpdateStrategy(bundle, item))
                            .ifPresent(strategy -> {
                                // try to install
                                EntandoDeBundle entandoDeBundle = kubernetesService.fetchBundleByName(bundleCode)
                                        .orElseThrow(() -> {
                                            log.debug("EntandoDeBundle not found with bundleCode:'{}'", bundleCode);
                                            return new BundleNotFoundException(bundleCode);
                                        });
                                EntandoDeBundleTag tag = getBundleTagOrFail(entandoDeBundle, item.getVersion());

                                EntandoBundleJobEntity job = installService.install(entandoDeBundle, tag, strategy);

                                Supplier<JobStatus> getJobStatus = () -> getEntandoBundleJobStatus(job.getId());

                                Set<JobStatus> errorStatuses = Set.of(JobStatus.INSTALL_ROLLBACK,
                                        JobStatus.INSTALL_ERROR);
                                Set<JobStatus> waitStatuses = new HashSet<>(errorStatuses);
                                waitStatuses.add(JobStatus.INSTALL_COMPLETED);

                                waitForJobStatus(getJobStatus, waitStatuses);

                                if (errorStatuses.contains(getJobStatus.get())) {
                                    throw new EntandoGeneralException("error installing " + item.getName());
                                }
                            });
                }

                log.info("Post init install executed successfully");
                status = PostInitStatus.SUCCESSFUL;
                retries = MAX_RETIES;
                finished = true;

            } catch (BundleNotFoundException ex) {
                log.info("Error Post init bundle install not found with bundle code:'{}'", getArgBundleIdentifier(ex));
                log.debug("BundleNotFoundException error:", ex);
                status = PostInitStatus.FAILED;
                retries = MAX_RETIES;
                finished = true;

            } catch (EntandoGeneralException | InvalidBundleException ex) {
                log.info("Error Post init bundle install with error message:'{}'", ex.getMessage());
                log.debug("EntandoGeneralException | InvalidBundleException error:", ex);
                status = PostInitStatus.FAILED;
                retries = MAX_RETIES;
                finished = true;

            } catch (Throwable ex) {
                // useful catch to manage executor bug
                log.error("Error unmanaged post init bundle install", ex.getMessage(), ex);
                status = PostInitStatus.UNKNOWN;
                finished = true;

            }

        } else {
            log.debug("Error Post init: EntandoApp not yet ready");
            status = PostInitStatus.UNKNOWN;
            finished = true;
        }
    }

    private String getArgBundleIdentifier(BundleNotFoundException ex) {
        return ex.getArgs() != null && ex.getArgs().length > 0 ? (String) ex.getArgs()[0] : "not present";
    }

    private PostInitItem checkActionOrSwitchToDefault(PostInitItem item) {
        if (StringUtils.isBlank(item.getAction())) {
            log.debug("For bundle :'{}' action is blank:'{}' switch do default:'{}'", item.getName(), item.getAction(),
                    DEFAULT_ACTION);
            item.setAction(DEFAULT_ACTION);
        }
        return item;
    }

    private JobStatus getEntandoBundleJobStatus(UUID id) {
        return entandoBundleJobService.getById(id.toString()).map(EntandoBundleJobEntity::getStatus).orElse(null);
    }


    public static void waitForJobStatus(Supplier<JobStatus> jobStatus, Set<JobStatus> expected) {
        await().atMost(MAX_WAITING_TIME_FOR_JOB_STATUS)
                .pollInterval(AWAITILY_DEFAULT_POLL_INTERVAL)
                .until(() -> expected.contains(jobStatus.get()));

        if (!expected.contains(jobStatus.get())) {
            log.info("Await installation timeout");
            throw new EntandoGeneralException("error wait install timeout");
        }
    }

    private EntandoBundle deployPostInitBundle(PostInitItem item) {
        log.info("Create a new CR and deploy it");
        BundleInfo bundleInfo = BundleInfo.builder()
                .name(item.getName())
                .bundleId(BundleUtilities.removeProtocolAndGetBundleId(item.getUrl()))
                .gitRepoAddress(item.getUrl())
                .build();
        return bundleService.deployDeBundle(bundleInfo);
    }

    private Optional<String> retrieveApplicationStatus() {
        try {
            String kubeStatus = kubernetesService.getCurrentAppStatusPhase();
            return Optional.ofNullable(kubeStatus);
        } catch (Exception ex) {
            log.warn("Error post init retrieve app status or bundle install:'{}'", ex.getMessage());
            return Optional.empty();
        }

    }

    private String calculateBundleCode(PostInitItem item) {
        String bundleId = BundleUtilities.removeProtocolAndGetBundleId(item.getUrl());

        // https://kubernetes.io/docs/concepts/overview/working-with-objects/names/#dns-subdomain-names
        if (StringUtils.length(item.getName()) > 200
                || !ValidationFunctions.VALID_CHARS_RFC_1123_REGEX_PATTERN.matcher(item.getName()).matches()
                || !ValidationFunctions.HOST_MUST_START_AND_END_WITH_ALPHANUMERIC_REGEX_PATTERN.matcher(item.getName())
                .matches()) {
            throw new InvalidBundleException("Error bundle name not valid (RFC 1123) " + item.getName());
        }
        String bundleName = item.getName();
        return BundleUtilities.composeBundleCode(bundleName, bundleId);
    }

    private Optional<InstallAction> computeInstallStrategy(EntandoBundle bundle, PostInitItem item) {
        if (isFirstInstall(bundle) && isInstallActionPlanned(item)) {
            log.info("Computed strategy: install `{}`", item.getName());
            return Optional.of(InstallAction.OVERRIDE);
        }
        return Optional.empty();
    }

    private boolean isFirstInstall(EntandoBundle bundle) {
        return !bundle.isInstalled();
    }

    private boolean ecrActionAllowed(PostInitItem item, String action) {
        boolean isActionAllowed =
                item.getEcrActions() != null && Arrays.stream(item.getEcrActions()).anyMatch(action::equals);
        log.trace("For bundle:'{}' action '{}' is allowed ? '{}'", item.getName(), action, isActionAllowed);
        return isActionAllowed;

    }

    private boolean isActionPlanned(PostInitItem item, String action) {
        boolean isActionPlanned = StringUtils.equals(item.getAction(), action);
        log.trace("For bundle:'{}' action '{}' is planned ? '{}'", item.getName(), action, isActionPlanned);
        return isActionPlanned;

    }

    private boolean isInstallActionPlanned(PostInitItem item) {
        return isActionPlanned(item, ACTION_INSTALL_OR_UPDATE);
    }

    private Optional<InstallAction> computeUpdateStrategy(EntandoBundle bundle, PostInitItem item) {
        if (isUpgrade(bundle, item.getVersion()) && isUpdateActionPlanned(item)) {
            log.info("Computed strategy: update `{}`", item.getName());
            return Optional.of(InstallAction.OVERRIDE);
        }
        return Optional.empty();
    }

    private boolean isUpgrade(EntandoBundle bundle, String versionToinstall) {
        return bundle.isInstalled() && !StringUtils.equals(versionToinstall,
                bundle.getInstalledJob().getComponentVersion());
    }

    private boolean isUpdateActionPlanned(PostInitItem item) {
        return isActionPlanned(item, ACTION_INSTALL_OR_UPDATE);
    }

    private EntandoDeBundleTag getBundleTagOrFail(EntandoDeBundle bundle, String versionToFind) {
        return Optional.ofNullable(bundle.getSpec().getTags())
                .flatMap(tags -> tags.stream().filter(t -> t.getVersion().equals(versionToFind)).findAny())
                .orElseThrow(
                        () -> new InvalidBundleException(
                                "Version " + versionToFind + " not defined in bundle versions"));
    }


    private Optional<PostInitData> parsePostInitConfiguration() {
        if (StringUtils.isBlank(postInitConfigurationData)) {
            return Optional.empty();
        } else {
            try {
                return Optional.ofNullable(mapper.readValue(postInitConfigurationData, PostInitData.class));
            } catch (JsonProcessingException ex) {
                log.warn("Error processing json input configuration data:'{}'", postInitConfigurationData, ex);
                return Optional.empty();
            }
        }
    }

    @Override
    public PostInitStatus getStatus() {
        return status;
    }

    @Override
    public boolean isCompleted() {
        return finished;
    }

    @ToString
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PostInitData {

        @Getter
        @Setter
        private int frequency = DEFAULT_CONFIGURATION_FREQUENCY;
        @Getter
        @Setter
        private int maxAppWait = DEFAULT_CONFIGURATION_TIMEOUT;
        @Getter
        private List<PostInitItem> items = new ArrayList<>();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PostInitItem {

        private String name;
        private String[] ecrActions;
        private String action;
        private String url;
        private String version;
        private int priority;
    }
}
