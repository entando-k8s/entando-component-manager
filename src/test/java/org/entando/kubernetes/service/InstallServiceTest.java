package org.entando.kubernetes.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.entando.kubernetes.assertionhelper.AnalysisReportAssertionHelper;
import org.entando.kubernetes.client.EntandoBundleComponentJobRepositoryTestDouble;
import org.entando.kubernetes.client.EntandoBundleJobRepositoryTestDouble;
import org.entando.kubernetes.client.core.EntandoCoreClient;
import org.entando.kubernetes.controller.digitalexchange.job.model.AnalysisReport;
import org.entando.kubernetes.model.bundle.ComponentType;
import org.entando.kubernetes.model.bundle.downloader.BundleDownloader;
import org.entando.kubernetes.model.bundle.downloader.BundleDownloaderFactory;
import org.entando.kubernetes.model.bundle.processor.AssetProcessor;
import org.entando.kubernetes.model.bundle.processor.CategoryProcessor;
import org.entando.kubernetes.model.bundle.processor.ComponentProcessor;
import org.entando.kubernetes.model.bundle.processor.ContentTypeProcessor;
import org.entando.kubernetes.model.bundle.processor.FileProcessor;
import org.entando.kubernetes.model.bundle.processor.FragmentProcessor;
import org.entando.kubernetes.model.bundle.processor.PluginProcessor;
import org.entando.kubernetes.model.bundle.reportable.AnalysisReportFunction;
import org.entando.kubernetes.model.bundle.reportable.Reportable;
import org.entando.kubernetes.model.bundle.reportable.ReportableComponentProcessor;
import org.entando.kubernetes.model.bundle.reportable.ReportableRemoteHandler;
import org.entando.kubernetes.model.debundle.EntandoDeBundle;
import org.entando.kubernetes.model.debundle.EntandoDeBundleSpec;
import org.entando.kubernetes.model.debundle.EntandoDeBundleSpecBuilder;
import org.entando.kubernetes.model.debundle.EntandoDeBundleTagBuilder;
import org.entando.kubernetes.model.entandocore.EntandoCoreComponentUsage;
import org.entando.kubernetes.model.job.EntandoBundleComponentJobEntity;
import org.entando.kubernetes.model.job.EntandoBundleEntity;
import org.entando.kubernetes.model.job.EntandoBundleJobEntity;
import org.entando.kubernetes.model.job.JobStatus;
import org.entando.kubernetes.model.job.JobType;
import org.entando.kubernetes.repository.EntandoBundleComponentJobRepository;
import org.entando.kubernetes.repository.EntandoBundleJobRepository;
import org.entando.kubernetes.repository.InstalledEntandoBundleRepository;
import org.entando.kubernetes.service.digitalexchange.component.EntandoBundleComponentUsageService;
import org.entando.kubernetes.service.digitalexchange.component.EntandoBundleService;
import org.entando.kubernetes.service.digitalexchange.job.EntandoBundleInstallService;
import org.entando.kubernetes.service.digitalexchange.job.EntandoBundleUninstallService;
import org.entando.kubernetes.stubhelper.AnalysisReportStubHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class InstallServiceTest {

    public static final String bundleFolder = InstallServiceTest.class.getResource("/bundle").getFile();
    public static final String BUNDLE_ID = "test-bundle";
    public static final String BUNDLE_TITLE = "bundle-title";
    public static final String BUNDLE_VERSION = "1.0";

    private EntandoBundleInstallService installService;
    private EntandoBundleUninstallService uninstallService;
    private EntandoBundleService bundleService;
    private BundleDownloader bundleDownloader;
    private BundleDownloaderFactory downloaderFactory;
    private EntandoBundleJobRepository jobRepository;
    private EntandoBundleComponentJobRepository compJobRepo;
    private InstalledEntandoBundleRepository installRepo;
    private Map<ComponentType, ComponentProcessor<?>> processorMap;
    private List<ReportableComponentProcessor> reportableComponentProcessorList;
    private Map<ReportableRemoteHandler, AnalysisReportFunction> analysisReportStrategies;
    private EntandoCoreClient coreClient;
    private KubernetesService kubernetesService;
    private EntandoBundleComponentUsageService usageService;

    @BeforeEach
    public void init() {
        downloaderFactory = new BundleDownloaderFactory();
        processorMap = new HashMap<>();
        reportableComponentProcessorList = new ArrayList<>();
        analysisReportStrategies = new HashMap<>();

        bundleService = Mockito.mock(EntandoBundleService.class);
        bundleDownloader = Mockito.mock(BundleDownloader.class);
        jobRepository = Mockito.spy(EntandoBundleJobRepositoryTestDouble.class);
        compJobRepo = Mockito.spy(EntandoBundleComponentJobRepositoryTestDouble.class);
        installRepo = Mockito.mock(InstalledEntandoBundleRepository.class);
        coreClient = Mockito.mock(EntandoCoreClient.class);
        kubernetesService = Mockito.mock(KubernetesService.class);
        usageService = Mockito.mock(EntandoBundleComponentUsageService.class);

        downloaderFactory.setDefaultSupplier(() -> bundleDownloader);

        installService = new EntandoBundleInstallService(
                bundleService, downloaderFactory, jobRepository, compJobRepo, installRepo, processorMap,
                reportableComponentProcessorList, analysisReportStrategies);

        uninstallService = new EntandoBundleUninstallService(
                jobRepository, compJobRepo, installRepo, usageService, processorMap);
    }


    @Test
    void receivingDataFromRemoteHandlerWillReturnTheRightAnalysisReport() {

        reportableComponentProcessorList.add(new FragmentProcessor(coreClient));
        reportableComponentProcessorList.add(new CategoryProcessor(coreClient));
        reportableComponentProcessorList.add(new PluginProcessor(kubernetesService));
        reportableComponentProcessorList.add(new AssetProcessor(coreClient));

        // instruct the strategy map with stub data
        analysisReportStrategies.put(ReportableRemoteHandler.ENTANDO_ENGINE,
                (List<Reportable> reportableList) -> AnalysisReportStubHelper
                        .stubAnalysisReportWithFragmentsAndCategories());
        analysisReportStrategies.put(ReportableRemoteHandler.ENTANDO_CMS,
                (List<Reportable> reportableList) -> AnalysisReportStubHelper
                        .stubAnalysisReportWithAssets());
        analysisReportStrategies.put(ReportableRemoteHandler.ENTANDO_K8S_SERVICE,
                (List<Reportable> reportableList) -> AnalysisReportStubHelper
                        .stubAnalysisReportWithPlugins());

        EntandoDeBundle bundle = getTestBundle();

        when(bundleDownloader.saveBundleLocally(any(), any())).thenReturn(Paths.get(bundleFolder));

        AnalysisReport analysisReport = installService
                .performInstallAnalysis(bundle, bundle.getSpec().getTags().get(0));

        AnalysisReportAssertionHelper.assertOnAnalysisReports(
                AnalysisReportStubHelper.stubAnalysisReportWithFragmentsAndCategoriesAndPluginsAndAssets(),
                analysisReport);

        Assertions.fail("add all processors");
    }


    @Test
    public void shouldIncrementProgressDuringInstallation() {
        processorMap.put(ComponentType.RESOURCE, new FileProcessor(coreClient));
        EntandoDeBundle bundle = getTestBundle();

        EntandoBundleEntity testEntity = EntandoBundleEntity.builder()
                .id(bundle.getMetadata().getName())
                .name(bundle.getSpec().getDetails().getName())
                .build();

        when(bundleDownloader.saveBundleLocally(any(), any())).thenReturn(Paths.get(bundleFolder));
        when(bundleService.convertToEntityFromEcr(any())).thenReturn(testEntity);

        EntandoBundleJobEntity job = installService.install(bundle, bundle.getSpec().getTags().get(0));

        await().atMost(5, TimeUnit.MINUTES)
                .pollInterval(5, TimeUnit.SECONDS)
                .until(() -> jobRepository.getOne(job.getId()).getStatus().isOfType(JobType.FINISHED));

        List<Double> progress = getJobProgress();
        assertThat(progress.size()).isEqualTo(6);
        assertThat(progress).containsExactly(0.0, 0.2, 0.4, 0.6, 0.8, 1.0);
    }

    @Test
    public void shouldIncrementProgressUpToLastInstalledWhenRollback() {
        processorMap.put(ComponentType.RESOURCE, new FileProcessor(coreClient));
        processorMap.put(ComponentType.CONTENT_TYPE, new ContentTypeProcessor(coreClient));

        EntandoDeBundle bundle = getTestBundle();

        EntandoBundleEntity testEntity = EntandoBundleEntity.builder()
                .id(bundle.getMetadata().getName())
                .name(bundle.getSpec().getDetails().getName())
                .build();

        when(bundleDownloader.saveBundleLocally(any(), any())).thenReturn(Paths.get(bundleFolder));
        when(bundleService.convertToEntityFromEcr(any())).thenReturn(testEntity);
        doThrow(RuntimeException.class).when(coreClient).createContentType(any());

        EntandoBundleJobEntity job = installService.install(bundle, bundle.getSpec().getTags().get(0));

        await().atMost(5, TimeUnit.MINUTES)
                .pollInterval(5, TimeUnit.SECONDS)
                .until(() -> jobRepository.getOne(job.getId()).getStatus().isOfType(JobType.FINISHED));

        List<Double> progress = getJobProgress();
        assertThat(progress.size()).isEqualTo(6);
        assertThat(progress).containsExactly(0.0, 0.16, 0.32, 0.48, 0.64, 0.8);

    }

    @Test
    public void shouldIncrementProgressDuringUninstall() {
        EntandoBundleEntity bundleEntity = EntandoBundleEntity.builder()
                .id(BUNDLE_ID)
                .name(BUNDLE_TITLE)
                .build();
        EntandoBundleJobEntity jobEntity = EntandoBundleJobEntity.builder()
                .componentId(BUNDLE_ID)
                .componentName(BUNDLE_TITLE)
                .componentVersion(BUNDLE_VERSION)
                .status(JobStatus.INSTALL_COMPLETED)
                .build();
        bundleEntity.setJob(jobEntity);

        EntandoBundleComponentJobEntity cjeA = new EntandoBundleComponentJobEntity();
        cjeA.setComponentId("A");
        cjeA.setComponentType(ComponentType.CONTENT_TYPE);
        EntandoBundleComponentJobEntity cjeB = new EntandoBundleComponentJobEntity();
        cjeB.setComponentId("B");
        cjeB.setComponentType(ComponentType.CONTENT_TYPE);

        processorMap.put(ComponentType.CONTENT_TYPE, new ContentTypeProcessor(coreClient));

        when(installRepo.findById(any())).thenReturn(Optional.of(bundleEntity));
        when(compJobRepo.findAllByParentJob(any())).thenReturn(Arrays.asList(cjeA, cjeB));
        when(usageService.getUsage(ComponentType.CONTENT_TYPE, "A"))
                .thenReturn(new EntandoCoreComponentUsage.NoUsageComponent(ComponentType.CONTENT_TYPE, "A"));
        when(usageService.getUsage(ComponentType.CONTENT_TYPE, "B"))
                .thenReturn(new EntandoCoreComponentUsage.NoUsageComponent(ComponentType.CONTENT_TYPE, "B"));

        EntandoBundleJobEntity job = uninstallService.uninstall(BUNDLE_ID);

        await().atMost(5, TimeUnit.MINUTES)
                .pollInterval(5, TimeUnit.SECONDS)
                .until(() -> jobRepository.getOne(job.getId()).getStatus().isOfType(JobType.FINISHED));

        List<Double> progress = getJobProgress();
        assertThat(progress.size()).isEqualTo(3);
        assertThat(progress).containsExactly(0.0, 0.5, 1.0);
    }

    private List<Double> getJobProgress() {
        List<Double> allProgresses = Mockito.mockingDetails(jobRepository).getInvocations()
                .stream().filter(i -> i.getMethod().getName().equals("save"))
                .map(i -> ((EntandoBundleJobEntity) i.getArgument(0)).getProgress())
                .collect(Collectors.toList());

        return getRealProgress(allProgresses);
    }


    private List<Double> getRealProgress(List<Double> allProgresses) {
        List<Double> validProgress = new ArrayList<>();

        for (int i = 0; i < allProgresses.size(); i++) {
            Double currentProgress = allProgresses.get(i);
            int lastValidProgressIndex = validProgress.size() - 1;
            if (lastValidProgressIndex < 0 || !currentProgress.equals(validProgress.get(lastValidProgressIndex))) {
                validProgress.add(currentProgress);
            }
        }
        return validProgress;
    }

    private EntandoDeBundle getTestBundle() {
        EntandoDeBundle bundle = new EntandoDeBundle();
        bundle.getMetadata().setName(BUNDLE_ID);
        EntandoDeBundleSpec bundleSpec = new EntandoDeBundleSpecBuilder()
                .withNewDetails()
                .withName(BUNDLE_TITLE)
                .endDetails()
                .withTags(
                        Collections.singletonList(new EntandoDeBundleTagBuilder().withVersion(BUNDLE_VERSION).build()))
                .build();
        bundle.setSpec(bundleSpec);
        return bundle;
    }
}
