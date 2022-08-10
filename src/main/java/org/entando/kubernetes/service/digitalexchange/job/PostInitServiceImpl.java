package org.entando.kubernetes.service.digitalexchange.job;

import static org.awaitility.Awaitility.await;
import static org.entando.kubernetes.service.digitalexchange.job.PostInitConfigurationService.ACTION_INSTALL_OR_UPDATE;

import java.time.Duration;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
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
import org.entando.kubernetes.model.job.EntandoBundleEntity.OperatorStarter;
import org.entando.kubernetes.model.job.EntandoBundleJobEntity;
import org.entando.kubernetes.model.job.JobStatus;
import org.entando.kubernetes.service.KubernetesService;
import org.entando.kubernetes.service.digitalexchange.BundleUtilities;
import org.entando.kubernetes.service.digitalexchange.component.EntandoBundleService;
import org.entando.kubernetes.service.digitalexchange.job.PostInitConfigurationService.PostInitItem;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PostInitServiceImpl implements PostInitService {

    public static final String DEFAULT_ACTION = "deploy-only";
    public static final String ECR_ACTION_UNINSTALL = "uninstall";
    private final EntandoBundleService bundleService;
    private final EntandoBundleInstallService installService;
    private final EntandoBundleUninstallService uninstallService;
    private final EntandoBundleJobService entandoBundleJobService;
    private final KubernetesService kubernetesService;
    private final PostInitConfigurationService postInitConfigurationService;
    private PostInitStatus status;
    private boolean finished;
    private static final int MAX_RETIES = 5000;
    private int retries = 0;
    private static final Duration MAX_WAITING_TIME_FOR_JOB_STATUS = Duration.ofSeconds(360);
    private static final Duration AWAITILY_DEFAULT_POLL_INTERVAL = Duration.ofSeconds(6);

    private Map<String, EntandoBundle> bundlesInstalledOrDeployed;

    public PostInitServiceImpl(PostInitConfigurationService postInitConfigurationService,
            EntandoBundleService bundleService, EntandoBundleInstallService installService,
            EntandoBundleUninstallService uninstallService,
            KubernetesService kubernetesService, EntandoBundleJobService entandoBundleJobService) {
        this.postInitConfigurationService = postInitConfigurationService;
        this.bundleService = bundleService;
        this.installService = installService;
        this.uninstallService = uninstallService;
        this.kubernetesService = kubernetesService;
        this.entandoBundleJobService = entandoBundleJobService;
    }

    @Override
    public boolean shouldRetry() {
        return retries < MAX_RETIES;
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

            List<PostInitItem> postInitBundleInConfiguration = postInitConfigurationService.getConfigurationData()
                    .getItems().stream()
                    .sorted(compareByPriorityAndThenName)
                    .collect(Collectors.toList());

            // list ALL bundles installed (for update) or not
            bundlesInstalledOrDeployed = bundleService.listBundles().getBody().stream()
                    .collect(Collectors.toMap(
                            EntandoBundle::getCode,
                            Function.identity(),
                            (item1, item2) -> item1));

            // list installed or uninstalled not undeployed PostInit bundle that must be removed
            List<EntandoBundle> postInitBundlesToRemove = bundleService.listInstalledOrRemovedPostInitBundles()
                    .getBody().stream()
                    .filter(bundle -> !isBundleInConfiguration(bundle))
                    .collect(Collectors.toList());

            try {

                for (PostInitItem itemFromConfig : postInitBundleInConfiguration) {
                    final PostInitItem item = checkActionOrSwitchToDefault(itemFromConfig);
                    log.info("Post init executing action '{}' on bundle '{}'", item.getAction(), item.getName());

                    Consumer<PostInitItem> postInitConsumer = computeActionToExecute(item);

                    postInitConsumer.accept(item);
                }

                for (EntandoBundle bundle : postInitBundlesToRemove) {
                    log.info("Post init try to remove bundle:'{}'", bundle.getCode());
                    if (bundle.isInstalled()) {
                        uninstallBundleAndWaitForCompletion(bundle.getCode());
                    }
                    bundleService.undeployDeBundle(bundle.getCode());
                }

                log.info("Post init install executed successfully");
                status = PostInitStatus.SUCCESSFUL;
                retries = MAX_RETIES;
                finished = true;

            } catch (BundleNotFoundException | EntandoGeneralException | InvalidBundleException ex) {
                log.info("Error Post init bundle install with error message:'{}'", ex.getMessage());
                log.debug("Bundle or generic error:", ex);
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

    private Consumer<PostInitItem> computeActionToExecute(PostInitItem item) {
        switch (item.getAction()) {
            case ACTION_INSTALL_OR_UPDATE:
                return this::installOrUpdateConfigurationItem;
            case DEFAULT_ACTION:
                return manageDeployOnlyAction(item);
            default:
                throw new EntandoGeneralException(String.format("Post init action:'%s' on bundle:'%s' not recognized",
                        item.getAction(), item.getName()));

        }
    }

    private Consumer<PostInitItem> manageDeployOnlyAction(PostInitItem item) {
        String bundleCode = PostInitServiceUtility.calculateBundleCode(item);
        EntandoBundle bundle = bundlesInstalledOrDeployed.get(bundleCode);
        if (bundle == null) {
            return this::deployPostInitBundle;
        } else if (bundle.isInstalled()) {
            return this::uninstallBundle;
        } else {
            return i -> log.debug("Post init action:'{}' on bundle:'{}' not installed do nothing",
                    i.getAction(),
                    i.getName());
        }
    }

    private void uninstallBundle(PostInitItem item) {
        log.info("Post init try to uninstall bundle:'{}'", item.getName());

        String bundleCode = PostInitServiceUtility.calculateBundleCode(item);

        uninstallBundleAndWaitForCompletion(bundleCode);
    }

    private void uninstallBundleAndWaitForCompletion(String bundleCode) {
        EntandoBundleJobEntity job = uninstallService.uninstall(bundleCode);

        Supplier<JobStatus> getJobStatus = () -> getEntandoBundleJobStatus(job.getId());

        Set<JobStatus> errorStatuses = Set.of(JobStatus.UNINSTALL_ERROR);
        Set<JobStatus> waitStatuses = new HashSet<>(errorStatuses);
        waitStatuses.add(JobStatus.UNINSTALL_COMPLETED);

        waitForJobStatus(getJobStatus, waitStatuses);

        if (errorStatuses.contains(getJobStatus.get())) {
            throw new EntandoGeneralException("error uninstalling " + bundleCode);
        }
    }

    private void installOrUpdateConfigurationItem(PostInitItem item) {
        String bundleCode = PostInitServiceUtility.calculateBundleCode(item);

        EntandoBundle bundle = Optional.ofNullable(bundlesInstalledOrDeployed.get(bundleCode))
                .orElseGet(() -> deployPostInitBundle(item));

        computeStrategy(bundle, item)
                .ifPresent(strategy -> {
                    log.info("Postinit try to install or update bundle:'{}'", item.getName());
                    // try to install
                    EntandoDeBundle entandoDeBundle = kubernetesService.fetchBundleByName(bundleCode)
                            .orElseThrow(() -> {
                                log.debug("EntandoDeBundle not found with bundleCode:'{}'", bundleCode);
                                return new BundleNotFoundException(bundleCode);
                            });

                    EntandoDeBundleTag tag = getBundleTagOrFail(entandoDeBundle, item.getVersion());

                    EntandoBundleJobEntity job = installService.install(entandoDeBundle, tag, strategy,
                            OperatorStarter.POST_INIT);

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

    private boolean isBundleInConfiguration(EntandoBundle bundle) {
        return postInitConfigurationService.getConfigurationData().getItems().stream()
                .map(PostInitServiceUtility::calculateBundleCode)
                .anyMatch(code -> StringUtils.equals(code, bundle.getCode()));
    }

    private Optional<InstallAction> computeStrategy(EntandoBundle bundle, PostInitItem item) {
        if (isInstall(bundle) || isUpgrade(bundle, item.getVersion())) {
            log.info("Computed strategy install/upgrade:'OVERRIDE' for item bundle name:'{}'", item.getName());
            return Optional.of(InstallAction.OVERRIDE);
        } else {
            log.debug("Computed strategy not install/upgrade for item bundle name:'{}'", item.getName());
            return Optional.empty();
        }
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


    private void waitForJobStatus(Supplier<JobStatus> jobStatus, Set<JobStatus> expected) {
        await().atMost(MAX_WAITING_TIME_FOR_JOB_STATUS)
                .pollInterval(AWAITILY_DEFAULT_POLL_INTERVAL)
                .until(() -> expected.contains(jobStatus.get()));

        if (!expected.contains(jobStatus.get())) {
            log.info("Await operation timeout");
            throw new EntandoGeneralException("error wait operation timeout");
        }
    }

    private EntandoBundle deployPostInitBundle(PostInitItem item) {
        log.info("Create a new CR and deploy it for item with bundle name:'{}'", item.getName());
        BundleInfo bundleInfo = BundleInfo.builder()
                .name(item.getName())
                .bundleId(BundleUtilities.removeProtocolAndGetBundleId(item.getUrl()))
                .gitRepoAddress(item.getUrl())
                .build();
        return bundleService.deployDeBundle(bundleInfo, OperatorStarter.POST_INIT);
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

    private boolean isInstall(EntandoBundle bundle) {
        return !bundle.isInstalled();
    }

    private boolean isUpgrade(EntandoBundle bundle, String versionToinstall) {
        return bundle.isInstalled() && !StringUtils.equals(versionToinstall,
                bundle.getInstalledJob().getComponentVersion());
    }

    private EntandoDeBundleTag getBundleTagOrFail(EntandoDeBundle bundle, String versionToFind) {
        return Optional.ofNullable(bundle.getSpec().getTags())
                .flatMap(tags -> tags.stream().filter(t -> t.getVersion().equals(versionToFind)).findAny())
                .orElseThrow(
                        () -> new InvalidBundleException(
                                "Version " + versionToFind + " not defined in bundle versions"));
    }


    @Override
    public PostInitStatus getStatus() {
        return status;
    }

    @Override
    public boolean isCompleted() {
        return finished;
    }

}
