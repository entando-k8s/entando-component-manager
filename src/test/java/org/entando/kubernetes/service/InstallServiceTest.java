package org.entando.kubernetes.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.entando.kubernetes.client.EntandoBundleComponentJobRepositoryTestDouble;
import org.entando.kubernetes.client.EntandoBundleJobRepositoryTestDouble;
import org.entando.kubernetes.client.core.EntandoCoreClient;
import org.entando.kubernetes.model.bundle.downloader.BundleDownloader;
import org.entando.kubernetes.model.bundle.downloader.BundleDownloaderFactory;
import org.entando.kubernetes.model.bundle.processor.ComponentProcessor;
import org.entando.kubernetes.model.bundle.processor.FileProcessor;
import org.entando.kubernetes.model.debundle.EntandoDeBundle;
import org.entando.kubernetes.model.debundle.EntandoDeBundleSpec;
import org.entando.kubernetes.model.debundle.EntandoDeBundleSpecBuilder;
import org.entando.kubernetes.model.debundle.EntandoDeBundleTagBuilder;
import org.entando.kubernetes.model.digitalexchange.ComponentType;
import org.entando.kubernetes.model.digitalexchange.EntandoBundleEntity;
import org.entando.kubernetes.model.job.EntandoBundleJobEntity;
import org.entando.kubernetes.model.job.JobType;
import org.entando.kubernetes.repository.EntandoBundleComponentJobRepository;
import org.entando.kubernetes.repository.EntandoBundleJobRepository;
import org.entando.kubernetes.repository.InstalledEntandoBundleRepository;
import org.entando.kubernetes.service.digitalexchange.component.EntandoBundleService;
import org.entando.kubernetes.service.digitalexchange.job.EntandoBundleInstallService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class InstallServiceTest {

    public static final String bundleFolder = InstallServiceTest.class.getResource("/bundle").getFile();

    private EntandoBundleInstallService installService;
    private EntandoBundleService bundleService;
    private BundleDownloader bundleDownloader;
    private BundleDownloaderFactory downloaderFactory;
    private EntandoBundleJobRepository jobRepository;
    private EntandoBundleComponentJobRepository compJobRepo;
    private InstalledEntandoBundleRepository installRepo;
    private Map<ComponentType, ComponentProcessor<?>> processorMap;
    private EntandoCoreClient coreClient;

    @BeforeEach
    public void init() {
        downloaderFactory = new BundleDownloaderFactory();
        processorMap = new HashMap<>();

        bundleService = Mockito.mock(EntandoBundleService.class);
        bundleDownloader = Mockito.mock(BundleDownloader.class);
        jobRepository = Mockito.spy(EntandoBundleJobRepositoryTestDouble.class);
        compJobRepo = Mockito.spy(EntandoBundleComponentJobRepositoryTestDouble.class);
        installRepo = Mockito.mock(InstalledEntandoBundleRepository.class);
        coreClient = Mockito.mock(EntandoCoreClient.class);

        processorMap.put(ComponentType.ASSET, new FileProcessor(coreClient));
        downloaderFactory.setDefaultSupplier(() -> bundleDownloader);

        installService = new EntandoBundleInstallService(
                bundleService, downloaderFactory, jobRepository, compJobRepo, installRepo, processorMap);
    }

    @Test
    public void shouldIncrementProgress() {
        EntandoDeBundle bundle = getTestBundle();
        List<Double> progress;

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

        progress = Mockito.mockingDetails(jobRepository).getInvocations()
                .stream().filter(i -> i.getMethod().getName().equals("save"))
                .map(i -> ((EntandoBundleJobEntity)i.getArgument(0)).getProgress())
                .collect(Collectors.toList());

        assertThat(progress.size()).isGreaterThanOrEqualTo(6);
        assertThat(progress).contains(0.0, 0.2, 0.4, 0.6, 0.8, 1.0);
    }

    private EntandoDeBundle getTestBundle() {
        EntandoDeBundle bundle = new EntandoDeBundle();
        bundle.getMetadata().setName("test-bundle");
        EntandoDeBundleSpec bundleSpec = new EntandoDeBundleSpecBuilder()
                .withNewDetails()
                .withName("bundle-title")
                .endDetails()
                .withTags(Collections.singletonList(new EntandoDeBundleTagBuilder().withVersion("1.0.0").build()))
                .build();
        bundle.setSpec(bundleSpec);
        return bundle;
    }
}
