package org.entando.kubernetes.service.digitalexchange.job;

import static java.util.Optional.ofNullable;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.zip.ZipFile;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.client.digitalexchange.DigitalExchangesClient;
import org.entando.kubernetes.controller.digitalexchange.component.DigitalExchangeComponent;
import org.entando.kubernetes.controller.digitalexchange.model.DigitalExchange;
import org.entando.kubernetes.exception.job.JobNotFoundException;
import org.entando.kubernetes.exception.job.JobPackageException;
import org.entando.kubernetes.exception.k8ssvc.K8SServiceClientException;
import org.entando.kubernetes.model.bundle.ZipReader;
import org.entando.kubernetes.model.bundle.descriptor.ComponentDescriptor;
import org.entando.kubernetes.model.bundle.installable.Installable;
import org.entando.kubernetes.model.bundle.processor.ComponentProcessor;
import org.entando.kubernetes.model.debundle.EntandoDeBundle;
import org.entando.kubernetes.model.debundle.EntandoDeBundleTag;
import org.entando.kubernetes.model.digitalexchange.DigitalExchangeJob;
import org.entando.kubernetes.model.digitalexchange.DigitalExchangeJobComponent;
import org.entando.kubernetes.model.digitalexchange.JobStatus;
import org.entando.kubernetes.repository.DigitalExchangeJobComponentRepository;
import org.entando.kubernetes.repository.DigitalExchangeJobRepository;
import org.entando.kubernetes.service.KubernetesService;
import org.entando.kubernetes.service.digitalexchange.component.DigitalExchangeComponentsService;
import org.entando.kubernetes.service.digitalexchange.signature.SignatureMatchingException;
import org.entando.kubernetes.service.digitalexchange.signature.SignatureUtil;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class DigitalExchangeInstallService implements ApplicationContextAware {

    private final @NonNull KubernetesService k8sService;
    private final @NonNull DigitalExchangeComponentsService digitalExchangeComponentsService;
    private final @NonNull DigitalExchangeJobRepository jobRepository;
    private final @NonNull DigitalExchangeJobComponentRepository componentRepository;
    private final ExecutorService threadPool = Executors.newFixedThreadPool(10, new ThreadFactoryBuilder()
            .setDaemon(true).setNameFormat("InstallableOperation-%d").build());


    private Collection<ComponentProcessor> componentProcessors = new ArrayList<>();

    public DigitalExchangeJob install(String componentId, String version) {
        EntandoDeBundle bundle = k8sService.getBundleByName(componentId)
                .orElseThrow(() -> new K8SServiceClientException("Bundle with name " + componentId + " not found in digital-exchange "));
        Optional<DigitalExchangeJob> existingJob = jobRepository
                .findByComponentIdAndStatusNotEqual(componentId, JobStatus.UNINSTALL_COMPLETED);

        if (existingJob.isPresent()) {
            return existingJob.get();
        }

        DigitalExchangeJob job = createInstallJob(bundle, version);

        submitInstallAsync(job, bundle, version);

        return job;
    }

    private DigitalExchangeJob createInstallJob(EntandoDeBundle bundle, String version) {
        final DigitalExchangeJob job = new DigitalExchangeJob();

        job.setComponentId(bundle.getSpec().getDetails().getName());
        job.setComponentName(bundle.getSpec().getDetails().getName());
        job.setComponentVersion(version);
        job.setDigitalExchange(bundle.getMetadata().getNamespace());
        job.setProgress(0);
        job.setStartedAt(LocalDateTime.now());
        job.setStatus(JobStatus.INSTALL_CREATED);

        jobRepository.save(job);
        return job;
    }

    public DigitalExchangeJob getJob(String componentId) {
        return jobRepository.findByComponentIdAndStatusNotEqual(componentId, JobStatus.UNINSTALL_COMPLETED)
                .orElseThrow(JobNotFoundException::new);
    }

    private void submitInstallAsync(DigitalExchangeJob job, EntandoDeBundle bundle, String version) {
        CompletableFuture.runAsync(() -> {
            jobRepository.updateJobStatus(job.getId(), JobStatus.INSTALL_IN_PROGRESS);
            CompletableFuture<InputStream> downloadComponentPackageStep = CompletableFuture
                    .supplyAsync(() -> downloadComponentPackage(bundle, version));

            CompletableFuture<Path> copyPackageLocallyStep = downloadComponentPackageStep
                    .thenApply(is -> savePackageStreamLocally(bundle.getSpec().getDetails().getName(), is));

            CompletableFuture<Path> verifySignatureStep = copyPackageLocallyStep.thenApply(tempPath -> {
//                TODO: Implement the verification by using the npm-signature which must be present in the tag
//                if (StringUtils.isNotEmpty(job.getDigitalExchange().getPublicKey())) {
//                    verifyDownloadedContentSignature(tempPath, digitalExchange, component);
//                }
                return tempPath;
            });

            CompletableFuture<List<Installable>> extractInstallableFromPackageStep = verifySignatureStep
                    .thenApply(p -> getInstallablesAndRemoveTempPackage(job, p));

            CompletableFuture<JobStatus> installComponentsStep = extractInstallableFromPackageStep
                    .thenApply(installableList -> processInstallableList(job, installableList) );

            CompletableFuture<JobStatus> handlePossibleErrorsStep = installComponentsStep
                    .exceptionally(this::handlePipelineException);

            handlePossibleErrorsStep.thenAccept(jobStatus -> jobRepository.updateJobStatus(job.getId(), jobStatus));

        });
    }

    private JobStatus handlePipelineException(Throwable th) {
        log.error("An error occurred during digital-exchange component installation", th.getCause());
        if (th.getCause() instanceof JobPackageException) {
            Path packagePath = ((JobPackageException) th.getCause()).getPackagePath();
            try {
                Files.deleteIfExists(packagePath);
            } catch (IOException e) {
                log.error("Impossible to clean local package file {} due to an exception", packagePath, e);
            }
        }
        return JobStatus.INSTALL_ERROR;
    }

    private JobStatus processInstallableList(DigitalExchangeJob job, List<Installable> installableList) {
        installableList.forEach(installable -> installable.setComponent(persistComponent(job, installable)));

        List<JobStatus> statuses = installableList.stream().map(installable -> {
            DigitalExchangeJobComponent installableComponent = installable.getComponent();
            componentRepository.updateJobStatus(installableComponent.getId(), JobStatus.INSTALL_IN_PROGRESS);

            CompletableFuture<?> future = installable.install();
            CompletableFuture<JobStatus> installResult = future.thenApply(vd -> {
                log.info("Installable '{}' finished successfully", installable.getName());
                componentRepository.updateJobStatus(installableComponent.getId(), JobStatus.INSTALL_COMPLETED);
                return JobStatus.INSTALL_COMPLETED;
            }).exceptionally(th -> {
                log.error("Installable '{}' has errors", installable.getName(), th.getCause());

                installableComponent.setStatus(JobStatus.INSTALL_ERROR);
                if (th.getCause() != null) {
                    String message = th.getCause().getMessage();
                    if (th.getCause() instanceof HttpClientErrorException) {
                        HttpClientErrorException httpException = (HttpClientErrorException) th.getCause();
                        message =
                                httpException.getMessage() + "\n" + httpException.getResponseBodyAsString();
                    }
                    componentRepository
                            .updateJobStatus(installableComponent.getId(), JobStatus.INSTALL_ERROR, message);
                } else {
                    componentRepository
                            .updateJobStatus(installableComponent.getId(), JobStatus.INSTALL_ERROR, th.getMessage());
                }
                return JobStatus.INSTALL_ERROR;
            });
            return installResult.join();
        }).collect(Collectors.toList());

        log.info("All have been processed");
        Optional<JobStatus> anyError = statuses.stream().filter(js -> js.equals(JobStatus.INSTALL_ERROR)).findAny();
        return anyError.orElse(JobStatus.INSTALL_COMPLETED);
    }

    private List<Installable> getInstallablesAndRemoveTempPackage(DigitalExchangeJob job, Path p) {
        try {
            ZipFile zip = new ZipFile(p.toFile());
            ZipReader zipReader = new ZipReader(zip);
            ComponentDescriptor descriptor = zipReader
                    .readDescriptorFile("descriptor.yaml", ComponentDescriptor.class);
            List<Installable> installableList = getInstallables(job, zipReader, descriptor);
            Files.delete(p);
            return installableList;
        } catch (IOException e) {
            throw new JobPackageException(p, "Unable to extract the list of installable from the zip file", e);
        }
    }

    private Path savePackageStreamLocally(String componentId, InputStream is) {
        Path tempPath = null;
        try {
            tempPath = Files.createTempFile(componentId, "");
            Files.copy(is, tempPath, StandardCopyOption.REPLACE_EXISTING);
            return tempPath;
        } catch (IOException e) {
            throw new JobPackageException(tempPath,
                    "An error occurred while copying the package stream locally", e);
        }
    }

    private InputStream downloadComponentPackage(EntandoDeBundle bundle, String version) {
        Optional<EntandoDeBundleTag> tag = bundle.getSpec().getTags().stream().filter(t -> t.getVersion().equals(version)).findFirst();
        String tarballUrl = tag
                .orElseThrow(() -> new RuntimeException("Version " + version + " not available for bundle " + bundle.getSpec().getDetails().getName() + " in digital-exchange " + bundle.getMetadata().getNamespace()))
                .getTarball();
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<Resource> responseEntity = restTemplate.exchange(
                tarballUrl, HttpMethod.GET, null, Resource.class);

        if (responseEntity.getBody() == null) {
            throw new HttpMessageNotReadableException("Response body is null");
        }

        try {
            return responseEntity.getBody().getInputStream();
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }

    }

    private void verifyDownloadedContentSignature(
            Path tempZipPath, DigitalExchange digitalExchange,
            DigitalExchangeComponent component) {

        try (InputStream in = Files.newInputStream(tempZipPath, StandardOpenOption.READ)) {
            boolean signatureMatches = SignatureUtil.verifySignature(
                    in, SignatureUtil.publicKeyFromPEM(digitalExchange.getPublicKey()),
                    component.getSignature());
            if (!signatureMatches) {
                throw new SignatureMatchingException("Component signature not matching public key");
            }
        } catch (IOException e) {
            throw new JobPackageException(tempZipPath, e);
        }
    }

    private List<Installable> getInstallables(DigitalExchangeJob job, ZipReader zipReader,
            ComponentDescriptor descriptor) throws IOException {
        List<Installable> installables = new LinkedList<>();
        for (ComponentProcessor processor : componentProcessors) {
            ofNullable(processor.process(job, zipReader, descriptor))
                    .ifPresent(installables::addAll);
        }
        return installables;
    }

    private DigitalExchangeJobComponent persistComponent(DigitalExchangeJob job, Installable installable) {
        DigitalExchangeJobComponent component = new DigitalExchangeJobComponent();
        component.setJob(job);
        component.setComponentType(installable.getComponentType());
        component.setName(installable.getName());
        component.setChecksum(installable.getChecksum());
        component.setStatus(JobStatus.INSTALL_CREATED);
        return componentRepository.save(component);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        componentProcessors = applicationContext.getBeansOfType(ComponentProcessor.class).values();
    }

}

