package org.entando.kubernetes.service.digitalexchange;

import static java.util.Optional.ofNullable;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.io.IOException;
import java.io.InputStream;
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
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.zip.ZipFile;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.entando.kubernetes.controller.digitalexchange.component.DigitalExchangeComponent;
import org.entando.kubernetes.exception.JobNotFoundException;
import org.entando.kubernetes.model.digitalexchange.DigitalExchangeEntity;
import org.entando.kubernetes.model.digitalexchange.DigitalExchangeJob;
import org.entando.kubernetes.model.digitalexchange.DigitalExchangeJobComponent;
import org.entando.kubernetes.model.digitalexchange.JobStatus;
import org.entando.kubernetes.repository.DigitalExchangeJobComponentRepository;
import org.entando.kubernetes.repository.DigitalExchangeJobRepository;
import org.entando.kubernetes.service.digitalexchange.client.DigitalExchangeBaseCall;
import org.entando.kubernetes.service.digitalexchange.client.DigitalExchangesClient;
import org.entando.kubernetes.service.digitalexchange.component.DigitalExchangeComponentsService;
import org.entando.kubernetes.service.digitalexchange.installable.ComponentProcessor;
import org.entando.kubernetes.service.digitalexchange.installable.Installable;
import org.entando.kubernetes.service.digitalexchange.job.JobExecutionException;
import org.entando.kubernetes.service.digitalexchange.job.JobPackageException;
import org.entando.kubernetes.service.digitalexchange.job.ZipReader;
import org.entando.kubernetes.service.digitalexchange.job.model.ComponentDescriptor;
import org.entando.kubernetes.service.digitalexchange.model.DigitalExchange;
import org.entando.kubernetes.service.digitalexchange.signature.SignatureMatchingException;
import org.entando.kubernetes.service.digitalexchange.signature.SignatureUtil;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

@Slf4j
@Service
@RequiredArgsConstructor
public class DigitalExchangeInstallService implements ApplicationContextAware {

    private final @NonNull DigitalExchangesService exchangesService;
    private final @NonNull DigitalExchangeComponentsService digitalExchangeComponentsService;
    private final @NonNull DigitalExchangesClient client;
    private final @NonNull DigitalExchangeJobRepository jobRepository;
    private final @NonNull DigitalExchangeJobComponentRepository componentRepository;
    private final ExecutorService POOL = Executors.newFixedThreadPool(10, new ThreadFactoryBuilder()
            .setDaemon(true).setNameFormat("InstallableOperation-%d").build());


    private Collection<ComponentProcessor> componentProcessors = new ArrayList<>();

    public DigitalExchangeJob install(String digitalExchangeId, String componentId) {
        DigitalExchangeEntity digitalExchange = exchangesService.findEntityById(digitalExchangeId);
        DigitalExchangeComponent component = digitalExchangeComponentsService
                .getComponent(digitalExchange.convert(), componentId).getPayload();
        Optional<DigitalExchangeJob> existingJob = jobRepository
                .findByComponentIdAndStatusNotEqual(componentId, JobStatus.UNINSTALL_COMPLETED);

        if (existingJob.isPresent()) {
            return existingJob.get();
        }

        DigitalExchangeJob job = new DigitalExchangeJob();

        job.setComponentId(componentId);
        job.setComponentName(component.getName());
        job.setComponentVersion(component.getVersion());
        job.setDigitalExchange(digitalExchange);
        job.setProgress(0);
        job.setStartedAt(LocalDateTime.now());
        job.setStatus(JobStatus.INSTALL_CREATED);

        jobRepository.save(job);

        submitInstallAsync(job, digitalExchange.convert(), component);

        return job;
    }

    public DigitalExchangeJob getJob(String componentId) {
        return jobRepository.findByComponentIdAndStatusNotEqual(componentId, JobStatus.UNINSTALL_COMPLETED)
                .orElseThrow(JobNotFoundException::new);
    }

    private void submitInstallAsync(DigitalExchangeJob job, DigitalExchange digitalExchange,
            DigitalExchangeComponent component) {
        CompletableFuture.runAsync(() -> {
            jobRepository.updateJobStatus(job.getId(), JobStatus.INSTALL_IN_PROGRESS);
            CompletableFuture<InputStream> downloadComponentPackageStep = CompletableFuture.supplyAsync(() -> {
                DigitalExchangeBaseCall<InputStream> call = new DigitalExchangeBaseCall<>(
                        HttpMethod.GET, "digitalExchange", "components", component.getId(), "package");
                return client.getStreamResponse(digitalExchange, call);
            });

            CompletableFuture<Path> copyPackageLocallyStep = downloadComponentPackageStep.thenApply(is -> {
                Path tempPath = null;
                try {
                    tempPath = Files.createTempFile(component.getId(), "");
                    Files.copy(is, tempPath, StandardCopyOption.REPLACE_EXISTING);
                    return tempPath;
                } catch (IOException e) {
                    throw new JobPackageException(tempPath,
                            "An error occurred while copying the package stream locally", e);
                }
            });

            CompletableFuture<Path> verifySignatureStep = copyPackageLocallyStep.thenApply(tempPath -> {
                if (StringUtils.isNotEmpty(job.getDigitalExchange().getPublicKey())) {
                    verifyDownloadedContentSignature(tempPath, digitalExchange, component);
                }
                return tempPath;
            });

            CompletableFuture<List<Installable>> extractInstallableFromPackageStep = verifySignatureStep.thenApply(p -> {
                        try  {
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
                    });

            CompletableFuture<JobStatus> installComponentsStep = extractInstallableFromPackageStep.thenApply(installableList -> {
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
//                            throw new JobExecutionException(th.getMessage(), th.getCause());
                            return JobStatus.INSTALL_ERROR;
                        });
                        return installResult.join();
                    }).collect(Collectors.toList());

//                    return CompletableFuture.allOf(completableFutures)
//                            .thenApply(vd -> JobStatus.INSTALL_COMPLETED)
//                            .exceptionally(th -> {
//                                log.error("Installation on package failed", th.getCause());
//                                return JobStatus.INSTALL_ERROR;
//                            }).join();
                    Optional<JobStatus> anyError = statuses.stream().filter(js -> js.equals(JobStatus.INSTALL_ERROR)).findAny();
                    return anyError.orElse(JobStatus.INSTALL_COMPLETED);
                });

            CompletableFuture<JobStatus> handlePossibleErrorsStep = installComponentsStep.exceptionally(th -> {
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
            });

            handlePossibleErrorsStep.thenAccept(jobStatus -> jobRepository.updateJobStatus(job.getId(), jobStatus));

        });
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

    private void extractZip(DigitalExchangeJob job, Path tempZipPath) {
        log.info("Processing DEPKG File");
        try (ZipFile zip = new ZipFile(tempZipPath.toFile())) {
            ZipReader zipReader = new ZipReader(zip);
            ComponentDescriptor descriptor = zipReader.readDescriptorFile("descriptor.yaml", ComponentDescriptor.class);
            List<Installable> installables = getInstallables(job, zipReader, descriptor);

            installables.forEach(installable -> installable.setComponent(persistComponent(job, installable)));

            new Thread(() -> {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                CompletableFuture[] completableFutures = installables.stream().map(installable -> {
                    DigitalExchangeJobComponent component = installable.getComponent();
                    componentRepository.updateJobStatus(component.getId(), JobStatus.INSTALL_IN_PROGRESS);

                    CompletableFuture<?> future = installable.install();
                    try {
                        future.get();
                        log.info("Installable '{}' finished successfully", installable.getName());
                        componentRepository.updateJobStatus(component.getId(), JobStatus.INSTALL_COMPLETED);
                    } catch (InterruptedException | ExecutionException ex) {
                        log.error("Installable '{}' has errors", installable.getName(), ex);
                        component.setStatus(JobStatus.INSTALL_ERROR);

                        if (ex.getCause() != null) {
                            String message = ex.getCause().getMessage();
                            if (ex.getCause() instanceof HttpClientErrorException) {
                                HttpClientErrorException httpException = (HttpClientErrorException) ex.getCause();
                                message = httpException.getMessage() + "\n" + httpException.getResponseBodyAsString();
                            }
                            componentRepository.updateJobStatus(component.getId(), JobStatus.INSTALL_ERROR, message);
                        } else {
                            componentRepository
                                    .updateJobStatus(component.getId(), JobStatus.INSTALL_ERROR, ex.getMessage());
                        }
                    }

                    return future;
                }).toArray(CompletableFuture[]::new);
                CompletableFuture.allOf(completableFutures).whenComplete((object, ex) -> {
                    JobStatus status = ex == null ? JobStatus.INSTALL_COMPLETED : JobStatus.INSTALL_ERROR;
                    jobRepository.updateJobStatus(job.getId(), status);
                });
            }).start();

            log.info("Finished processing. Have a nice day!");
            // add labels, etc
        } catch (IOException ex) {
            log.error("Error while extracting zip file", ex);
            throw new JobExecutionException("Unable to extract zip file", ex);
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
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        componentProcessors = applicationContext.getBeansOfType(ComponentProcessor.class).values();
    }

}

