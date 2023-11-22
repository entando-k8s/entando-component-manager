package org.entando.kubernetes.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
import org.entando.kubernetes.assertionhelper.InstallPlanAssertionHelper;
import org.entando.kubernetes.client.EntandoBundleComponentJobRepositoryTestDouble;
import org.entando.kubernetes.client.EntandoBundleJobRepositoryTestDouble;
import org.entando.kubernetes.client.core.EntandoCoreClient;
import org.entando.kubernetes.client.model.EntandoCoreComponentDeleteRequest;
import org.entando.kubernetes.client.model.EntandoCoreComponentDeleteResponse;
import org.entando.kubernetes.client.model.EntandoCoreComponentDeleteResponse.EntandoCoreComponentDelete;
import org.entando.kubernetes.client.model.EntandoCoreComponentDeleteResponse.EntandoCoreComponentDeleteResponseStatus;
import org.entando.kubernetes.client.model.EntandoCoreComponentDeleteResponse.EntandoCoreComponentDeleteStatus;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallAction;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallPlan;
import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.exception.digitalexchange.BundleOperationConcurrencyException;
import org.entando.kubernetes.exception.digitalexchange.ReportAnalysisException;
import org.entando.kubernetes.model.bundle.ComponentType;
import org.entando.kubernetes.model.bundle.downloader.BundleDownloader;
import org.entando.kubernetes.model.bundle.downloader.BundleDownloaderFactory;
import org.entando.kubernetes.model.bundle.downloader.DownloadedBundle;
import org.entando.kubernetes.model.bundle.processor.AssetProcessor;
import org.entando.kubernetes.model.bundle.processor.CategoryProcessor;
import org.entando.kubernetes.model.bundle.processor.ComponentProcessor;
import org.entando.kubernetes.model.bundle.processor.ContentProcessor;
import org.entando.kubernetes.model.bundle.processor.ContentTemplateProcessor;
import org.entando.kubernetes.model.bundle.processor.ContentTypeProcessor;
import org.entando.kubernetes.model.bundle.processor.DirectoryProcessor;
import org.entando.kubernetes.model.bundle.processor.FileProcessor;
import org.entando.kubernetes.model.bundle.processor.FragmentProcessor;
import org.entando.kubernetes.model.bundle.processor.GroupProcessor;
import org.entando.kubernetes.model.bundle.processor.LabelProcessor;
import org.entando.kubernetes.model.bundle.processor.LanguageProcessor;
import org.entando.kubernetes.model.bundle.processor.PageProcessor;
import org.entando.kubernetes.model.bundle.processor.PageTemplateProcessor;
import org.entando.kubernetes.model.bundle.processor.PluginProcessor;
import org.entando.kubernetes.model.bundle.processor.WidgetProcessor;
import org.entando.kubernetes.model.bundle.reportable.AnalysisReportFunction;
import org.entando.kubernetes.model.bundle.reportable.Reportable;
import org.entando.kubernetes.model.bundle.reportable.ReportableComponentProcessor;
import org.entando.kubernetes.model.bundle.reportable.ReportableRemoteHandler;
import org.entando.kubernetes.model.bundle.usage.ComponentUsage;
import org.entando.kubernetes.model.debundle.EntandoDeBundle;
import org.entando.kubernetes.model.debundle.EntandoDeBundleSpec;
import org.entando.kubernetes.model.debundle.EntandoDeBundleSpecBuilder;
import org.entando.kubernetes.model.debundle.EntandoDeBundleTag;
import org.entando.kubernetes.model.debundle.EntandoDeBundleTagBuilder;
import org.entando.kubernetes.model.job.EntandoBundleComponentJobEntity;
import org.entando.kubernetes.model.job.EntandoBundleEntity;
import org.entando.kubernetes.model.job.EntandoBundleJobEntity;
import org.entando.kubernetes.model.job.JobStatus;
import org.entando.kubernetes.model.job.JobType;
import org.entando.kubernetes.repository.ComponentDataRepository;
import org.entando.kubernetes.repository.EntandoBundleComponentJobRepository;
import org.entando.kubernetes.repository.EntandoBundleJobRepository;
import org.entando.kubernetes.repository.InstalledEntandoBundleRepository;
import org.entando.kubernetes.repository.PluginDataRepository;
import org.entando.kubernetes.service.digitalexchange.component.EntandoBundleComponentUsageService;
import org.entando.kubernetes.service.digitalexchange.component.EntandoBundleService;
import org.entando.kubernetes.service.digitalexchange.concurrency.BundleOperationsConcurrencyManager;
import org.entando.kubernetes.service.digitalexchange.crane.CraneCommand;
import org.entando.kubernetes.service.digitalexchange.job.BundleUninstallUtility;
import org.entando.kubernetes.service.digitalexchange.job.EntandoBundleInstallService;
import org.entando.kubernetes.service.digitalexchange.job.EntandoBundleUninstallService;
import org.entando.kubernetes.service.digitalexchange.job.PostInitService;
import org.entando.kubernetes.service.digitalexchange.templating.WidgetTemplateGeneratorService;
import org.entando.kubernetes.stubhelper.AnalysisReportStubHelper;
import org.entando.kubernetes.stubhelper.BundleInfoStubHelper;
import org.entando.kubernetes.stubhelper.BundleStatusItemStubHelper;
import org.entando.kubernetes.stubhelper.InstallPlanStubHelper;
import org.entando.kubernetes.utils.TenantPrimaryContextJunitExt;
import org.entando.kubernetes.validator.descriptor.BundleDescriptorValidator;
import org.entando.kubernetes.validator.descriptor.PageDescriptorValidator;
import org.entando.kubernetes.validator.descriptor.PluginDescriptorValidator;
import org.entando.kubernetes.validator.descriptor.WidgetDescriptorValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

@Tag("unit")
@ExtendWith(TenantPrimaryContextJunitExt.class)
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
    private PostInitService postInitService;
    private Map<ComponentType, ComponentProcessor<?>> processorMap;
    private List<ReportableComponentProcessor> reportableComponentProcessorList;
    private Map<ReportableRemoteHandler, AnalysisReportFunction> analysisReportStrategies;
    private EntandoCoreClient coreClient;
    private KubernetesService kubernetesService;
    private EntandoBundleComponentUsageService usageService;
    private BundleOperationsConcurrencyManager bundleOperationsConcurrencyManager;
    private PluginDescriptorValidator pluginDescriptorValidator;
    private PluginDataRepository pluginDataRepository;
    private ComponentDataRepository componentDataRepository;
    private WidgetTemplateGeneratorService templateGeneratorService;
    private WidgetDescriptorValidator widgetDescriptorValidator;
    private PageDescriptorValidator pageDescriptorValidator;
    private BundleDescriptorValidator bundleDescriptorValidator;
    private CraneCommand craneCommand;
    private DownloadedBundle downloadedBundle = new DownloadedBundle(Paths.get(bundleFolder), "");
    private BundleUninstallUtility bundleUninstallUtility;

    @BeforeEach
    public void init() {
        downloaderFactory = new BundleDownloaderFactory();
        processorMap = new HashMap<>();
        reportableComponentProcessorList = new ArrayList<>();
        analysisReportStrategies = new HashMap<>();

        bundleService = mock(EntandoBundleService.class);
        bundleDownloader = mock(BundleDownloader.class);
        jobRepository = Mockito.spy(EntandoBundleJobRepositoryTestDouble.class);
        compJobRepo = Mockito.spy(EntandoBundleComponentJobRepositoryTestDouble.class);
        installRepo = mock(InstalledEntandoBundleRepository.class);
        postInitService = mock(PostInitService.class);
        coreClient = mock(EntandoCoreClient.class);
        kubernetesService = mock(KubernetesService.class);
        usageService = mock(EntandoBundleComponentUsageService.class);
        bundleOperationsConcurrencyManager = mock(BundleOperationsConcurrencyManager.class);
        pluginDescriptorValidator = mock(PluginDescriptorValidator.class);
        when(pluginDescriptorValidator.getFullDeploymentNameMaxlength()).thenReturn(200);
        pluginDataRepository = mock(PluginDataRepository.class);
        componentDataRepository = mock(ComponentDataRepository.class);
        templateGeneratorService = mock(WidgetTemplateGeneratorService.class);
        widgetDescriptorValidator = mock(WidgetDescriptorValidator.class);
        pageDescriptorValidator = mock(PageDescriptorValidator.class);
        bundleDescriptorValidator = mock(BundleDescriptorValidator.class);
        craneCommand = mock(CraneCommand.class);
        bundleUninstallUtility = mock(BundleUninstallUtility.class);
        downloaderFactory.setDefaultSupplier(() -> bundleDownloader);

        installService = new EntandoBundleInstallService(
                bundleService, downloaderFactory, jobRepository, compJobRepo, installRepo, processorMap,
                reportableComponentProcessorList, analysisReportStrategies, bundleOperationsConcurrencyManager,
                bundleDescriptorValidator, coreClient, bundleUninstallUtility);

        uninstallService = new EntandoBundleUninstallService(
                jobRepository, compJobRepo, installRepo, usageService, postInitService, processorMap, coreClient,
                bundleUninstallUtility);

        when(bundleOperationsConcurrencyManager.manageStartOperation()).thenReturn(true);
    }


    @Test
    void receivingDataFromRemoteHandlerWillReturnTheRightInstallPlan() {

        when(pluginDescriptorValidator.ensureDescriptorVersionIsSet(any())).thenCallRealMethod();

        reportableComponentProcessorList.add(new AssetProcessor(coreClient));
        reportableComponentProcessorList.add(new CategoryProcessor(coreClient));
        reportableComponentProcessorList.add(new ContentProcessor(coreClient));
        reportableComponentProcessorList.add(new ContentTemplateProcessor(coreClient));
        reportableComponentProcessorList.add(new ContentTypeProcessor(coreClient));
        reportableComponentProcessorList.add(new DirectoryProcessor(coreClient));
        reportableComponentProcessorList.add(new FileProcessor(coreClient));
        reportableComponentProcessorList.add(new FragmentProcessor(coreClient));
        reportableComponentProcessorList.add(new GroupProcessor(coreClient));
        reportableComponentProcessorList.add(new LabelProcessor(coreClient));
        reportableComponentProcessorList.add(new LanguageProcessor(coreClient));
        reportableComponentProcessorList.add(new PageProcessor(coreClient, pageDescriptorValidator));
        reportableComponentProcessorList.add(new PageTemplateProcessor(coreClient));
        reportableComponentProcessorList.add(new PluginProcessor(kubernetesService, pluginDescriptorValidator,
                pluginDataRepository, craneCommand));
        reportableComponentProcessorList.add(
                new WidgetProcessor(componentDataRepository, coreClient, templateGeneratorService,
                        widgetDescriptorValidator));

        // instruct the strategy map with stub data
        analysisReportStrategies.put(ReportableRemoteHandler.ENTANDO_ENGINE,
                (List<Reportable> reportableList) -> AnalysisReportStubHelper
                        .stubFullEngineAnalysisReport());
        analysisReportStrategies.put(ReportableRemoteHandler.ENTANDO_CMS,
                (List<Reportable> reportableList) -> AnalysisReportStubHelper
                        .getCmsAnalysisReport());
        analysisReportStrategies.put(ReportableRemoteHandler.ENTANDO_K8S_SERVICE,
                (List<Reportable> reportableList) -> AnalysisReportStubHelper
                        .stubFullK8SServiceAnalysisReport());

        EntandoDeBundle bundle = getTestBundle();

        when(bundleDownloader.saveBundleLocally(any(), any())).thenReturn(downloadedBundle);

        InstallPlan installPlan = installService.generateInstallPlan(bundle, bundle.getSpec().getTags().get(0),
                true);

        InstallPlanAssertionHelper.assertOnInstallPlan(
                InstallPlanStubHelper.stubFullInstallPlan(),
                installPlan);
    }


    @Test
    void ifAnErrorOccurDuringReportAnalysisFlowItShouldThrowReportAnalysisException() {

        reportableComponentProcessorList.add(new CategoryProcessor(coreClient));

        // instruct the strategy map with stub data
        analysisReportStrategies.put(ReportableRemoteHandler.ENTANDO_ENGINE, coreClient::getEngineAnalysisReport);
        analysisReportStrategies.put(ReportableRemoteHandler.ENTANDO_CMS,
                (List<Reportable> reportableList) -> AnalysisReportStubHelper
                        .getCmsAnalysisReport());

        EntandoDeBundle bundle = getTestBundle();

        when(bundleDownloader.saveBundleLocally(any(), any())).thenReturn(downloadedBundle);
        when(coreClient.getEngineAnalysisReport(anyList())).thenThrow(ReportAnalysisException.class);

        EntandoDeBundleTag entandoDeBundleTag = bundle.getSpec().getTags().get(0);

        assertThrows(ReportAnalysisException.class,
                () -> installService.generateInstallPlan(bundle, entandoDeBundleTag,
                        true));
    }


    @Test
    public void shouldIncrementProgressDuringInstallation() {

        processorMap.put(ComponentType.RESOURCE, new FileProcessor(coreClient));
        EntandoDeBundle bundle = getTestBundle();

        EntandoBundleEntity testEntity = EntandoBundleEntity.builder()
                .bundleCode(bundle.getMetadata().getName())
                .name(bundle.getSpec().getDetails().getName())
                .build();

        when(bundleDownloader.saveBundleLocally(any(), any())).thenReturn(downloadedBundle);
        when(bundleService.convertToEntityFromEcr(any())).thenReturn(testEntity);

        EntandoBundleJobEntity job = installService.install(bundle, bundle.getSpec().getTags().get(0));

        await().atMost(5, TimeUnit.MINUTES)
                .pollInterval(5, TimeUnit.SECONDS)
                .until(() -> jobRepository.getOne(job.getId()).getStatus().isOfType(JobType.FINISHED));

        List<Double> progress = getJobProgress();
        assertThat(progress).containsExactly(0.0, 0.2, 0.4, 0.6, 0.8, 1.0);
    }

    @Test
    public void shouldIncrementProgressUpToLastInstalledWhenRollback() {
        processorMap.put(ComponentType.RESOURCE, new FileProcessor(coreClient));
        processorMap.put(ComponentType.CONTENT_TYPE, new ContentTypeProcessor(coreClient));

        EntandoDeBundle bundle = getTestBundle();

        EntandoBundleEntity testEntity = EntandoBundleEntity.builder()
                .bundleCode(bundle.getMetadata().getName())
                .name(bundle.getSpec().getDetails().getName())
                .build();

        when(bundleDownloader.saveBundleLocally(any(), any())).thenReturn(downloadedBundle);
        when(bundleService.convertToEntityFromEcr(any())).thenReturn(testEntity);
        doThrow(RuntimeException.class).when(coreClient).createContentType(any());

        EntandoBundleJobEntity job = installService.install(bundle, bundle.getSpec().getTags().get(0));

        await().atMost(5, TimeUnit.MINUTES)
                .pollInterval(5, TimeUnit.SECONDS)
                .until(() -> jobRepository.getOne(job.getId()).getStatus().isOfType(JobType.FINISHED));

        List<Double> progress = getJobProgress();
        assertThat(progress).containsExactly(0.0, 0.14, 0.28, 0.42, 0.56, 0.7);

    }

    @Test
    void shouldSkipUninstall_ForPostInitBundle() {
        EntandoBundleEntity bundleEntity = EntandoBundleEntity.builder()
                .bundleCode(BUNDLE_ID)
                .name(BUNDLE_TITLE)
                .build();
        EntandoBundleJobEntity jobEntity = EntandoBundleJobEntity.builder()
                .componentId(BUNDLE_ID)
                .componentName(BUNDLE_TITLE)
                .componentVersion(BUNDLE_VERSION)
                .status(JobStatus.INSTALL_COMPLETED)
                .build();
        bundleEntity.setJob(jobEntity);

        when(installRepo.findByBundleCode(any())).thenReturn(Optional.of(bundleEntity));
        when(postInitService.isEcrActionAllowed(any(), any())).thenReturn(Optional.of(Boolean.FALSE));

        assertThrows(EntandoComponentManagerException.class, () -> uninstallService.uninstall(BUNDLE_ID));

    }

    @Test
    void shouldNotSkipUninstall_ForPostInitBundle() {
        EntandoBundleEntity bundleEntity = EntandoBundleEntity.builder()
                .bundleCode(BUNDLE_ID)
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
        cjeA.setAction(InstallAction.CREATE);
        cjeA.setComponentType(ComponentType.CONTENT_TYPE);
        EntandoBundleComponentJobEntity cjeB = new EntandoBundleComponentJobEntity();
        cjeB.setComponentId("B");
        cjeB.setAction(InstallAction.CREATE);
        cjeB.setComponentType(ComponentType.CONTENT_TYPE);

        processorMap.put(ComponentType.CONTENT_TYPE, new ContentTypeProcessor(coreClient));

        when(installRepo.findByBundleCode(any())).thenReturn(Optional.of(bundleEntity));
        when(postInitService.isEcrActionAllowed(any(), any())).thenReturn(Optional.of(Boolean.TRUE));
        when(compJobRepo.findAllByParentJob(any())).thenReturn(Arrays.asList(cjeA, cjeB));
        when(usageService.getComponentsUsageDetails(any())).thenReturn(Arrays.asList(
                ComponentUsage.builder().code("A").type(ComponentType.CONTENT_TYPE).exist(true).references(List.of()).usage(0).build(),
                ComponentUsage.builder().code("B").type(ComponentType.CONTENT_TYPE).exist(true).references(List.of()).usage(0).build()));
        when(coreClient.deleteComponents(Arrays.asList(
                new EntandoCoreComponentDeleteRequest(ComponentType.CONTENT_TYPE, "A"),
                new EntandoCoreComponentDeleteRequest(ComponentType.CONTENT_TYPE, "B"))))
                .thenReturn(EntandoCoreComponentDeleteResponse.builder().status(
                        EntandoCoreComponentDeleteResponseStatus.SUCCESS).build());

        EntandoBundleJobEntity job = uninstallService.uninstall(BUNDLE_ID);

        await().atMost(5, TimeUnit.MINUTES)
                .pollInterval(5, TimeUnit.SECONDS)
                .until(() -> jobRepository.getOne(job.getId()).getStatus().isOfType(JobType.FINISHED));

        List<Double> progress = getJobProgress();
        assertThat(progress).containsExactly(0.0, 0.33, 0.66, 1.0);

    }

    @Test
    public void shouldIncrementProgressDuringUninstall() {
        EntandoBundleEntity bundleEntity = EntandoBundleEntity.builder()
                .bundleCode(BUNDLE_ID)
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
        cjeA.setAction(InstallAction.CREATE);
        cjeA.setComponentType(ComponentType.CONTENT_TYPE);
        EntandoBundleComponentJobEntity cjeB = new EntandoBundleComponentJobEntity();
        cjeB.setComponentId("B");
        cjeB.setAction(InstallAction.CREATE);
        cjeB.setComponentType(ComponentType.CONTENT_TYPE);

        processorMap.put(ComponentType.CONTENT_TYPE, new ContentTypeProcessor(coreClient));

        when(installRepo.findByBundleCode(any())).thenReturn(Optional.of(bundleEntity));
        when(compJobRepo.findAllByParentJob(any())).thenReturn(Arrays.asList(cjeA, cjeB));
        when(usageService.getComponentsUsageDetails(any())).thenReturn(Arrays.asList(
                ComponentUsage.builder().code("A").type(ComponentType.CONTENT_TYPE).exist(true).references(List.of()).usage(0).build(),
                ComponentUsage.builder().code("B").type(ComponentType.CONTENT_TYPE).exist(true).references(List.of()).usage(0).build()));
        when(coreClient.deleteComponents(Arrays.asList(
                new EntandoCoreComponentDeleteRequest(ComponentType.CONTENT_TYPE, "A"),
                new EntandoCoreComponentDeleteRequest(ComponentType.CONTENT_TYPE, "B"))))
                .thenReturn(EntandoCoreComponentDeleteResponse.builder().status(
                        EntandoCoreComponentDeleteResponseStatus.SUCCESS).build());

        EntandoBundleJobEntity job = uninstallService.uninstall(BUNDLE_ID);

        await().atMost(5, TimeUnit.MINUTES)
                .pollInterval(5, TimeUnit.SECONDS)
                .until(() -> jobRepository.getOne(job.getId()).getStatus().isOfType(JobType.FINISHED));

        List<Double> progress = getJobProgress();
        assertThat(progress).containsExactly(0.0, 0.33, 0.66, 1.0);

        compJobRepo.findAll().forEach(componentJobEntity -> {
            assertNull(componentJobEntity.getUninstallErrorMessage());
            assertNull(componentJobEntity.getUninstallErrorCode());
        });

    }

    @Test
    void shouldSetUninstallErrorIfDeleteResponseIsFailure() {
        EntandoBundleEntity bundleEntity = EntandoBundleEntity.builder()
                .bundleCode(BUNDLE_ID)
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
        cjeA.setAction(InstallAction.CREATE);
        cjeA.setComponentType(ComponentType.CONTENT_TYPE);
        EntandoBundleComponentJobEntity cjeB = new EntandoBundleComponentJobEntity();
        cjeB.setComponentId("B");
        cjeB.setAction(InstallAction.CREATE);
        cjeB.setComponentType(ComponentType.CONTENT_TYPE);

        processorMap.put(ComponentType.CONTENT_TYPE, new ContentTypeProcessor(coreClient));

        when(installRepo.findByBundleCode(any())).thenReturn(Optional.of(bundleEntity));
        when(postInitService.isEcrActionAllowed(any(), any())).thenReturn(Optional.of(Boolean.TRUE));
        when(compJobRepo.findAllByParentJob(any())).thenReturn(Arrays.asList(cjeA, cjeB));
        when(usageService.getComponentsUsageDetails(any())).thenReturn(Arrays.asList(
                ComponentUsage.builder().code("A").type(ComponentType.CONTENT_TYPE).exist(true).references(List.of()).usage(0).build(),
                ComponentUsage.builder().code("B").type(ComponentType.CONTENT_TYPE).exist(true).references(List.of()).usage(0).build()));
        when(coreClient.deleteComponents(Arrays.asList(
                new EntandoCoreComponentDeleteRequest(ComponentType.CONTENT_TYPE, "A"),
                new EntandoCoreComponentDeleteRequest(ComponentType.CONTENT_TYPE, "B"))))
                .thenReturn(EntandoCoreComponentDeleteResponse.builder().status(
                        EntandoCoreComponentDeleteResponseStatus.FAILURE).components(Arrays.asList(
                        new EntandoCoreComponentDelete(ComponentType.CONTENT_TYPE, "A", EntandoCoreComponentDeleteStatus.FAILURE),
                        new EntandoCoreComponentDelete(ComponentType.CONTENT_TYPE, "B", EntandoCoreComponentDeleteStatus.FAILURE))).build());

        ArgumentCaptor<EntandoBundleComponentJobEntity> ac = ArgumentCaptor.forClass(EntandoBundleComponentJobEntity.class);

        EntandoBundleJobEntity job = uninstallService.uninstall(BUNDLE_ID);

        await().atMost(5, TimeUnit.MINUTES)
                .pollInterval(5, TimeUnit.SECONDS)
                .until(() -> jobRepository.getOne(job.getId()).getStatus().isOfType(JobType.FINISHED));

        List<Double> progress = getJobProgress();
        assertThat(progress).containsExactly(0.0, 0.33, 0.66, 1.0);
        EntandoBundleJobEntity bundleJobEntity = jobRepository.getOne(job.getId());
        assertEquals("UNINSTALL_ERROR", bundleJobEntity.getStatus().name());
    }

    @Test
    void shouldSetUninstallPartialCompletedIfDeleteResponseIsPartialSuccess() {
        EntandoBundleEntity bundleEntity = EntandoBundleEntity.builder()
                .bundleCode(BUNDLE_ID)
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
        cjeA.setAction(InstallAction.CREATE);
        cjeA.setComponentType(ComponentType.CONTENT_TYPE);
        EntandoBundleComponentJobEntity cjeB = new EntandoBundleComponentJobEntity();
        cjeB.setComponentId("B");
        cjeB.setAction(InstallAction.CREATE);
        cjeB.setComponentType(ComponentType.CONTENT_TYPE);

        processorMap.put(ComponentType.CONTENT_TYPE, new ContentTypeProcessor(coreClient));

        when(installRepo.findByBundleCode(any())).thenReturn(Optional.of(bundleEntity));
        when(postInitService.isEcrActionAllowed(any(), any())).thenReturn(Optional.of(Boolean.TRUE));
        when(compJobRepo.findAllByParentJob(any())).thenReturn(Arrays.asList(cjeA, cjeB));
        when(usageService.getComponentsUsageDetails(any())).thenReturn(Arrays.asList(
                ComponentUsage.builder().code("A").type(ComponentType.CONTENT_TYPE).exist(true).references(List.of()).usage(0).build(),
                ComponentUsage.builder().code("B").type(ComponentType.CONTENT_TYPE).exist(true).references(List.of()).usage(0).build()));
        when(coreClient.deleteComponents(Arrays.asList(
                new EntandoCoreComponentDeleteRequest(ComponentType.CONTENT_TYPE, "A"),
                new EntandoCoreComponentDeleteRequest(ComponentType.CONTENT_TYPE, "B"))))
                .thenReturn(EntandoCoreComponentDeleteResponse.builder().status(
                        EntandoCoreComponentDeleteResponseStatus.PARTIAL_SUCCESS).components(Arrays.asList(
                        new EntandoCoreComponentDelete(ComponentType.CONTENT_TYPE, "A", EntandoCoreComponentDeleteStatus.SUCCESS),
                        new EntandoCoreComponentDelete(ComponentType.CONTENT_TYPE, "B", EntandoCoreComponentDeleteStatus.FAILURE))).build());

        ArgumentCaptor<EntandoBundleComponentJobEntity> ac = ArgumentCaptor.forClass(EntandoBundleComponentJobEntity.class);

        EntandoBundleJobEntity job = uninstallService.uninstall(BUNDLE_ID);

        await().atMost(5, TimeUnit.MINUTES)
                .pollInterval(5, TimeUnit.SECONDS)
                .until(() -> jobRepository.getOne(job.getId()).getStatus().isOfType(JobType.FINISHED));

        List<Double> progress = getJobProgress();
        assertThat(progress).containsExactly(0.0, 0.33, 0.66, 1.0);
        EntandoBundleJobEntity bundleJobEntity = jobRepository.getOne(job.getId());
        assertEquals("UNINSTALL_PARTIAL_COMPLETED", bundleJobEntity.getStatus().name());

    }



    @Test
    void shouldSetUnistallErrorIfErrorRecordingFails() {
        EntandoBundleEntity bundleEntity = EntandoBundleEntity.builder()
                .bundleCode(BUNDLE_ID)
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
        cjeA.setAction(InstallAction.CREATE);
        cjeA.setComponentType(ComponentType.CONTENT_TYPE);
        EntandoBundleComponentJobEntity cjeB = new EntandoBundleComponentJobEntity();
        cjeB.setComponentId("B");
        cjeB.setAction(InstallAction.CREATE);
        cjeB.setComponentType(ComponentType.CONTENT_TYPE);

        processorMap.put(ComponentType.CONTENT_TYPE, new ContentTypeProcessor(coreClient));

        when(installRepo.findByBundleCode(any())).thenReturn(Optional.of(bundleEntity));
        when(postInitService.isEcrActionAllowed(any(), any())).thenReturn(Optional.of(Boolean.TRUE));
        when(compJobRepo.findAllByParentJob(any())).thenReturn(Arrays.asList(cjeA, cjeB));
        when(usageService.getComponentsUsageDetails(any())).thenReturn(Arrays.asList(
                ComponentUsage.builder().code("A").type(ComponentType.CONTENT_TYPE).exist(true).references(List.of()).usage(0).build(),
                ComponentUsage.builder().code("B").type(ComponentType.CONTENT_TYPE).exist(true).references(List.of()).usage(0).build()));
        when(coreClient.deleteComponents(Arrays.asList(
                new EntandoCoreComponentDeleteRequest(ComponentType.CONTENT_TYPE, "A"),
                new EntandoCoreComponentDeleteRequest(ComponentType.CONTENT_TYPE, "B"))))
                .thenReturn(EntandoCoreComponentDeleteResponse.builder().status(
                        EntandoCoreComponentDeleteResponseStatus.PARTIAL_SUCCESS).components(Arrays.asList(
                        new EntandoCoreComponentDelete(ComponentType.CONTENT_TYPE, "A", EntandoCoreComponentDeleteStatus.SUCCESS),
                        new EntandoCoreComponentDelete(ComponentType.CONTENT_TYPE, "C", EntandoCoreComponentDeleteStatus.FAILURE))).build());

        doThrow(IllegalStateException.class).when(bundleUninstallUtility).markSingleErrors(any(),any());
        EntandoBundleJobEntity job = uninstallService.uninstall(BUNDLE_ID);

        await().atMost(5, TimeUnit.MINUTES)
                .pollInterval(5, TimeUnit.SECONDS)
                .until(() -> jobRepository.getOne(job.getId()).getStatus().isOfType(JobType.FINISHED));

        List<Double> progress = getJobProgress();
        assertThat(progress).containsExactly(0.0, 0.33, 0.66);
        EntandoBundleJobEntity bundleJobEntity = jobRepository.getOne(job.getId());
        assertEquals("UNINSTALL_ERROR", bundleJobEntity.getStatus().name());

    }

    @Test
    void shouldThrowBundleOperationConcurrencyExceptionWhenAnalysisOrInstallRequestedWhileAnotherOperationIsRunning() {

        // analysis
        doThrow(BundleOperationConcurrencyException.class).when(bundleOperationsConcurrencyManager)
                .throwIfAnotherOperationIsRunningOrStartOperation();
        assertThrows(BundleOperationConcurrencyException.class,
                () -> installService.generateInstallPlan(null, null, true));

        // install
        doThrow(BundleOperationConcurrencyException.class).when(bundleOperationsConcurrencyManager)
                .throwIfAnotherOperationIsRunningOrStartOperation();

        assertThrows(BundleOperationConcurrencyException.class,
                () -> installService.install(null, null));
    }

    @Test
    void shouldDoConcurrencyChecksWhenTrueIsReceived() {

        doNothing().when(bundleOperationsConcurrencyManager).throwIfAnotherOperationIsRunningOrStartOperation();
        doNothing().when(bundleOperationsConcurrencyManager).operationTerminated();
        try {
            installService.generateInstallPlan(null, null, true);
        } catch (Exception e) {
            // catch exception to avoid mocking everything uselessly
        }
        verify(bundleOperationsConcurrencyManager, times(1)).throwIfAnotherOperationIsRunningOrStartOperation();
        verify(bundleOperationsConcurrencyManager, times(1)).operationTerminated();
    }

    @Test
    void shouldNotDoConcurrencyChecksWhenFalseIsReceived() {

        doNothing().when(bundleOperationsConcurrencyManager).throwIfAnotherOperationIsRunningOrStartOperation();
        doNothing().when(bundleOperationsConcurrencyManager).operationTerminated();

        try {
            installService.generateInstallPlan(null, null, false);
        } catch (Exception e) {
            // catch exception to avoid mocking everything uselessly
        }
        verify(bundleOperationsConcurrencyManager, times(0)).throwIfAnotherOperationIsRunningOrStartOperation();
        verify(bundleOperationsConcurrencyManager, times(0)).operationTerminated();
    }

    @Test
    void shouldCorrectlyProcessPbcNames() {
        processorMap.put(ComponentType.RESOURCE, new FileProcessor(coreClient));
        EntandoDeBundle bundle = getTestBundle();

        EntandoBundleEntity testEntity = EntandoBundleEntity.builder()
                .bundleCode(bundle.getMetadata().getName())
                .name(bundle.getSpec().getDetails().getName())
                .build();

        when(bundleDownloader.saveBundleLocally(any(), any())).thenReturn(downloadedBundle);
        when(bundleService.convertToEntityFromEcr(any())).thenReturn(testEntity);

        EntandoBundleJobEntity job = installService.install(bundle, bundle.getSpec().getTags().get(0));


        await().atMost(5, TimeUnit.MINUTES)
                .pollInterval(5, TimeUnit.SECONDS)
                .until(() -> jobRepository.getOne(job.getId()).getStatus().isOfType(JobType.FINISHED));

        final ArgumentCaptor<EntandoBundleEntity> captor = ArgumentCaptor.forClass(EntandoBundleEntity.class);
        verify(installRepo, times(1)).save(captor.capture());
        final EntandoBundleEntity value = captor.getValue();
        assertThat(value.getPbcList()).isEqualTo(BundleInfoStubHelper.GROUPS_NAME.stream().collect(Collectors.joining(",")));
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
        bundle.getMetadata().setAnnotations(Map.of("entando.org/pbc", BundleInfoStubHelper.PBC_ANNOTATION_VALUE));
        EntandoDeBundleSpec bundleSpec = new EntandoDeBundleSpecBuilder()
                .withNewDetails()
                .withName(BUNDLE_TITLE)
                .endDetails()
                .withTags(
                        Collections.singletonList(new EntandoDeBundleTagBuilder().withVersion(BUNDLE_VERSION)
                                .withTarball(BundleStatusItemStubHelper.ID_DEPLOYED).build()))
                .build();
        bundle.setSpec(bundleSpec);
        return bundle;
    }
}
