package org.entando.kubernetes.service.digitalexchange.job;

import static java.util.Optional.ofNullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.exception.job.JobConflictException;
import org.entando.kubernetes.exception.job.JobPackageException;
import org.entando.kubernetes.exception.k8ssvc.K8SServiceClientException;
import org.entando.kubernetes.model.bundle.BundleReader;
import org.entando.kubernetes.model.bundle.descriptor.ComponentDescriptor;
import org.entando.kubernetes.model.bundle.installable.Installable;
import org.entando.kubernetes.model.bundle.processor.ComponentProcessor;
import org.entando.kubernetes.model.debundle.EntandoDeBundle;
import org.entando.kubernetes.model.debundle.EntandoDeBundleSpec;
import org.entando.kubernetes.model.debundle.EntandoDeBundleTag;
import org.entando.kubernetes.model.digitalexchange.DigitalExchangeComponent;
import org.entando.kubernetes.model.digitalexchange.DigitalExchangeJob;
import org.entando.kubernetes.model.digitalexchange.DigitalExchangeJobComponent;
import org.entando.kubernetes.model.digitalexchange.JobStatus;
import org.entando.kubernetes.model.digitalexchange.JobType;
import org.entando.kubernetes.repository.DigitalExchangeInstalledComponentRepository;
import org.entando.kubernetes.repository.DigitalExchangeJobComponentRepository;
import org.entando.kubernetes.repository.DigitalExchangeJobRepository;
import org.entando.kubernetes.service.KubernetesService;
import org.entando.kubernetes.service.digitalexchange.BundleUtilities;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
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
    private final @NonNull DigitalExchangeJobService jobService;
    private final @NonNull DigitalExchangeInstalledComponentRepository installedComponentRepository;
    private final @NonNull DigitalExchangeJobRepository jobRepository;
    private final @NonNull DigitalExchangeJobComponentRepository componentRepository;
    private Collection<ComponentProcessor> componentProcessors = new ArrayList<>();

    public DigitalExchangeJob install(String componentId, String version) {
        EntandoDeBundle bundle = k8sService.getBundleByName(componentId)
                .orElseThrow(() -> new K8SServiceClientException("Bundle with name " + componentId + " not found"));
        Optional<DigitalExchangeJob> optionalExistingJob = getExistingJob(bundle);

        if (optionalExistingJob.isPresent()) {
            DigitalExchangeJob j = optionalExistingJob.get();
            JobStatus js = j.getStatus();
            if (js.equals(JobStatus.INSTALL_COMPLETED)) {
                return j;
            }
            if (JobType.isOfType(js, JobType.UNFINISHED)) {
                throw new JobConflictException("Conflict with another job for the component " + j.getComponentId()
                        + " - JOB ID: " + j.getId());
            }
        }

        EntandoDeBundleTag versionToInstall = getBundleTag(bundle, version)
                .orElseThrow(() -> new RuntimeException("Provided version is not available for package"));
        DigitalExchangeJob job = createInstallJob(bundle, versionToInstall);

        submitInstallAsync(job, bundle, versionToInstall);

        return job;
    }

    private Optional<DigitalExchangeJob> getExistingJob(EntandoDeBundle bundle) {
        String digitalExchangeId = bundle.getMetadata().getNamespace();
        String componentId = bundle.getSpec().getDetails().getName();
        Optional<DigitalExchangeJob> lastJobStarted = jobRepository
                .findFirstByDigitalExchangeAndComponentIdOrderByStartedAtDesc(digitalExchangeId, componentId);
        if (lastJobStarted.isPresent()) {
            // To be an existing job it should be Running or completed
            if (lastJobStarted.get().getStatus() == JobStatus.UNINSTALL_COMPLETED) {
                return Optional.empty();
            }
            return lastJobStarted;
        }
        return Optional.empty();
    }

    private Optional<EntandoDeBundleTag> getBundleTag(EntandoDeBundle bundle, String version) {
        String versionToFind = BundleUtilities.getBundleVersionOrFail(bundle, version);
        return bundle.getSpec().getTags().stream().filter(t -> t.getVersion().equals(versionToFind)).findAny();
    }

    private DigitalExchangeJob createInstallJob(EntandoDeBundle bundle, EntandoDeBundleTag tag) {
        final DigitalExchangeJob job = new DigitalExchangeJob();

        job.setComponentId(bundle.getMetadata().getName());
        job.setComponentName(bundle.getSpec().getDetails().getName());
        job.setComponentVersion(tag.getVersion());
        job.setDigitalExchange(bundle.getMetadata().getNamespace());
        job.setProgress(0);
        job.setStartedAt(LocalDateTime.now());
        job.setStatus(JobStatus.INSTALL_CREATED);

        jobRepository.save(job);
        return job;
    }

    public List<DigitalExchangeJob> getAllJobs(String componentId) {
        return jobService.getAllJobs(componentId);
    }

    private void submitInstallAsync(DigitalExchangeJob job, EntandoDeBundle bundle, EntandoDeBundleTag tag) {
        CompletableFuture.runAsync(() -> {
            jobRepository.updateJobStatus(job.getId(), JobStatus.INSTALL_IN_PROGRESS);
            CompletableFuture<InputStream> downloadComponentPackageStep = CompletableFuture
                    .supplyAsync(() -> downloadComponentPackage(tag));

            CompletableFuture<Path> copyPackageLocallyStep = downloadComponentPackageStep
                    .thenApply(is -> savePackageStreamLocally(job.getComponentId(), is));

            CompletableFuture<Path> verifySignatureStep = copyPackageLocallyStep.thenApply(tempPath ->  tempPath);

            CompletableFuture<List<Installable>> extractInstallableFromPackageStep = verifySignatureStep
                    .thenApply(p -> getInstallablesAndRemoveTempPackage(job, p));

            CompletableFuture<JobStatus> installComponentsStep = extractInstallableFromPackageStep
                    .thenApply(installableList -> processInstallableList(job, installableList));

            CompletableFuture<JobStatus> saveInstalledComponentDatabaseEntry = installComponentsStep
                    .thenApply(js -> {
                        if (js.equals(JobStatus.INSTALL_COMPLETED)) {
                            DigitalExchangeComponent installedComponent = DigitalExchangeComponent.newFrom(bundle);
                            installedComponent.setInstalled(true);
                            installedComponentRepository.save(installedComponent);
                        }
                        return js;
                    });

            CompletableFuture<JobStatus> handlePossibleErrorsStep = saveInstalledComponentDatabaseEntry
                    .exceptionally(this::handlePipelineException);


            handlePossibleErrorsStep.thenAccept(jobStatus -> {
                jobRepository.updateJobStatus(job.getId(), jobStatus);
            });


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

        List<JobStatus> statuses = installableList.stream().map(this::processInstallable).collect(Collectors.toList());

        log.info("All have been processed");
        Optional<JobStatus> anyError = statuses.stream().filter(js -> js.equals(JobStatus.INSTALL_ERROR)).findAny();
        return anyError.orElse(JobStatus.INSTALL_COMPLETED);
    }

    private JobStatus processInstallable(Installable installable) {
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
    }

    private List<Installable> getInstallablesAndRemoveTempPackage(DigitalExchangeJob job, Path p) {
        try {
            BundleReader r = new BundleReader(p);
            ComponentDescriptor descriptor = r.readBundleDescriptor();
            List<Installable> installableList = getInstallables(job, r, descriptor);
            r.destroy();
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

    private InputStream downloadComponentPackage(EntandoDeBundleTag tag) {
        String tarballUrl = tag.getTarball();

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.add("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/54.0.2840.99 Safari/537.36");
        headers.add("Accept", "*/*");
        HttpEntity<String> entity = new HttpEntity<>("parameters", headers);

        ResponseEntity<Resource> responseEntity =  restTemplate.exchange(
                tarballUrl, HttpMethod.GET, entity, Resource.class);

        if (responseEntity.getBody() == null) {
            throw new HttpMessageNotReadableException("Response body is null");
        }

        try {
            return responseEntity.getBody().getInputStream();
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }

    }

    private List<Installable> getInstallables(DigitalExchangeJob job, BundleReader r,
            ComponentDescriptor descriptor) throws IOException {
        List<Installable> installables = new LinkedList<>();
        for (ComponentProcessor processor : componentProcessors) {
            ofNullable(processor.process(job, r, descriptor))
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

