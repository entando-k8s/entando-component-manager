package org.entando.kubernetes.service.digitalexchange.job;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
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
import org.entando.kubernetes.exception.digitalexchange.InvalidBundleException;
import org.entando.kubernetes.exception.k8ssvc.BundleNotFoundException;
import org.entando.kubernetes.model.bundle.BundleInfo;
import org.entando.kubernetes.model.bundle.EntandoBundle;
import org.entando.kubernetes.model.common.EntandoDeploymentPhase;
import org.entando.kubernetes.model.debundle.EntandoDeBundle;
import org.entando.kubernetes.model.debundle.EntandoDeBundleTag;
import org.entando.kubernetes.service.KubernetesService;
import org.entando.kubernetes.service.digitalexchange.BundleUtilities;
import org.entando.kubernetes.service.digitalexchange.component.EntandoBundleService;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PostInitServiceImpl implements PostInitService, InitializingBean {

    private final EntandoBundleService bundleService;
    private final EntandoBundleInstallService installService;
    private final KubernetesService kubernetesService;
    private final String postInitConfigurationData;
    private static final ObjectMapper mapper = new ObjectMapper();

    private PostInitStatus status;
    private boolean finished;
    private static final int MAX_RETIES = 100;
    private int retries = 0;
    private PostInitData configurationData;
    private static final PostInitData DEFAULT_CONFIGURATION_DATA;

    static {
        List<PostInitItem> items = new ArrayList<>();
        items.add(PostInitItem.builder()
                .name("entando-post-init-01")
                .url("docker://docker.io/entando/post-init")
                .version("0.0.1")
                .priority(1)
                .build());
        DEFAULT_CONFIGURATION_DATA = PostInitData.builder().frequency(3).items(items).build();

    }

    public PostInitServiceImpl(@Value("${entando.ecr.postinit:#{null}}") String postInitConfigurationData,
            EntandoBundleService bundleService, EntandoBundleInstallService installService,
            KubernetesService kubernetesService) {
        this.postInitConfigurationData = postInitConfigurationData;
        this.bundleService = bundleService;
        this.installService = installService;
        this.kubernetesService = kubernetesService;
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
    public PostInitData getConfigurationData() {
        return configurationData;
    }

    @Override
    public int getFrequencyInSeconds() {
        return configurationData.getFrequency();
    }

    @Override
    public Optional<Boolean> isBundleOperationAllowed(String bundleCode, String operation) {
        return configurationData.getItems().stream()
                .filter(item -> StringUtils.equals(calculateBundleCode(item), bundleCode))
                .findFirst()
                .map(item -> Boolean.valueOf(bundleOperationAllowed(item, operation)))
                .or(Optional::empty);
    }

    @Override
    public void install() {
        log.info("Post init phase install executing");

        finished = false;
        status = PostInitStatus.STARTED;
        retries++;

        Optional<String> appStatus = retrieveApplicationStatus();
        if (appStatus.isPresent() && EntandoDeploymentPhase.SUCCESSFUL.toValue().toLowerCase()
                .equals(appStatus.get())) {
            // sort bundle ootb to manage
            List<PostInitItem> bundleToInstall = configurationData.getItems().stream()
                    .sorted(Comparator.comparingInt(PostInitItem::getPriority))
                    .collect(Collectors.toList());

            // list ALL bundles installed (for update) or not
            Map<String, EntandoBundle> bundlesInstalledOrDeployed = bundleService.listBundles().getBody().stream()
                    .collect(Collectors.toMap(
                            EntandoBundle::getCode,
                            Function.identity(),
                            (item1, item2) -> item1
                    ));

            try {

                for (PostInitItem item : bundleToInstall) {
                    log.info("Post init installing item name:'{}'", item.getName());
                    if (StringUtils.isNotBlank(item.getAction())) {
                        String bundleCode = calculateBundleCode(item);

                        EntandoBundle bundle = bundlesInstalledOrDeployed.getOrDefault(bundleCode,
                                deployPostInitBundle(item));

                        computeInstallStrategy(bundle, item)
                                .or(() -> computeUpdateStrategy(bundle, item))
                                .ifPresent(strategy -> {
                                    // try to install
                                    EntandoDeBundle entandoDeBundle = kubernetesService.fetchBundleByName(bundleCode)
                                            .orElseThrow(() -> new BundleNotFoundException(bundleCode));
                                    EntandoDeBundleTag tag = getBundleTagOrFail(entandoDeBundle, item.getVersion());

                                    installService.install(entandoDeBundle, tag, strategy);
                                });
                    }
                }

                log.info("Post init phase install executed successfully");
                status = PostInitStatus.SUCCESSFUL;
                retries = MAX_RETIES;
                finished = true;

            } catch (BundleNotFoundException | InvalidBundleException ex) {
                log.debug("Error post init bundle install:'{}'", ex.getMessage());
                status = PostInitStatus.FAILED;
                retries = MAX_RETIES;
                finished = true;

            }

        } else {
            log.debug("Error post application status unknown");
            status = PostInitStatus.UNKNOWN;
            finished = true;
        }
    }

    private EntandoBundle deployPostInitBundle(PostInitItem item) {
        // FIXME maybe needs more data ?
        BundleInfo bundleInfo = BundleInfo.builder()
                .name(item.getName())
                .bundleId(BundleUtilities.removeProtocolAndGetBundleId(item.getUrl()))
                .gitRepoAddress(item.getUrl())
                //.version(item.getVersion())
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
        // FIXME check name max lenght or char content?
        String bundleName = item.getName();
        return BundleUtilities.composeBundleCode(bundleName, bundleId);
    }

    private Optional<InstallAction> computeInstallStrategy(EntandoBundle bundle, PostInitItem item) {
        if (isFirstInstall(bundle) && isInstallActionAllowed(item)) {
            return Optional.of(InstallAction.CREATE);
        }
        return Optional.empty();
    }

    private boolean isFirstInstall(EntandoBundle bundle) {
        return !bundle.isInstalled();
    }

    private boolean bundleOperationAllowed(PostInitItem item, String operation) {
        return item.getEcrActions() != null && Arrays.stream(item.getEcrActions()).anyMatch(operation::equals);

    }

    private boolean actionAllowed(PostInitItem item, String action) {
        return StringUtils.equals(item.getAction(), action);

    }

    private boolean isInstallActionAllowed(PostInitItem item) {
        return actionAllowed(item, "install-or-update");
    }

    private Optional<InstallAction> computeUpdateStrategy(EntandoBundle bundle, PostInitItem item) {
        if (isUpgrade(bundle, item.getVersion()) && isUpdateActionAllowed(item)) {
            return Optional.of(InstallAction.OVERRIDE);
        }
        return Optional.empty();
    }

    private boolean isUpgrade(EntandoBundle bundle, String versionToinstall) {
        return bundle.isInstalled() && StringUtils.equals(versionToinstall,
                bundle.getInstalledJob().getComponentVersion());
    }

    private boolean isUpdateActionAllowed(PostInitItem item) {
        return actionAllowed(item, "install-or-update");
    }

    private EntandoDeBundleTag getBundleTagOrFail(EntandoDeBundle bundle, String versionToFind) {
        return bundle.getSpec().getTags().stream().filter(t -> t.getVersion().equals(versionToFind)).findAny()
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
        private int frequency;
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
