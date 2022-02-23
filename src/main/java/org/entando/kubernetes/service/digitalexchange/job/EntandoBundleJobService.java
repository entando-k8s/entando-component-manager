package org.entando.kubernetes.service.digitalexchange.job;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.exception.job.JobConflictException;
import org.entando.kubernetes.model.debundle.EntandoDeBundle;
import org.entando.kubernetes.model.job.EntandoBundleComponentJobEntity;
import org.entando.kubernetes.model.job.EntandoBundleJobEntity;
import org.entando.kubernetes.model.job.JobStatus;
import org.entando.kubernetes.model.job.JobType;
import org.entando.kubernetes.model.web.request.PagedListRequest;
import org.entando.kubernetes.model.web.response.PagedMetadata;
import org.entando.kubernetes.repository.EntandoBundleComponentJobRepository;
import org.entando.kubernetes.repository.EntandoBundleJobRepository;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EntandoBundleJobService {

    private final @NonNull EntandoBundleJobRepository jobRepository;
    private final @NonNull EntandoBundleComponentJobRepository componentJobRepository;

    @EventListener
    public void onContextRefreshEvent(ContextRefreshedEvent event) {
        jobRepository.setStatusToInstallErrorWhenStatusIsInstallInProgress();
    }

    public PagedMetadata<EntandoBundleJobEntity> getJobs(PagedListRequest request) {
        List<EntandoBundleJobEntity> allJobs = getJobs();
        List<EntandoBundleJobEntity> filteredList = new EntandoBundleJobListProcessor(request, allJobs)
                .filterAndSort().toList();
        List<EntandoBundleJobEntity> sublist = request.getSublist(filteredList);

        return new PagedMetadata<>(request, sublist, filteredList.size());
    }

    public List<EntandoBundleJobEntity> getJobs() {
        return jobRepository.findAllByOrderByStartedAtDesc();
    }

    public List<EntandoBundleJobEntity> getJobs(String componentId) {
        return jobRepository.findAllByComponentIdOrderByStartedAtDesc(componentId);
    }

    public List<EntandoBundleComponentJobEntity> getJobRelatedComponentJobs(EntandoBundleJobEntity job) {
        return componentJobRepository.findAllByParentJob(job);
    }

    public Optional<EntandoBundleJobEntity> getById(String jobId) {
        return jobRepository.findById(UUID.fromString(jobId));
    }

    public Optional<EntandoBundleJobEntity> getComponentLastJobOfType(String componentId, JobType type) {
        return jobRepository.findFirstByComponentIdAndStatusInOrderByStartedAtDesc(componentId, type.getStatuses());
    }

    public Optional<EntandoBundleJobEntity> getComponentLastJobWithStatus(String componentId, JobStatus status) {
        return jobRepository.findFirstByComponentIdAndStatusOrderByStartedAtDesc(componentId, status);
    }

    public Optional<EntandoBundleJobEntity> findCompletedOrConflictingInstallJob(EntandoDeBundle bundle) {

        log.info("Verify validity of a new install job for component " + bundle.getMetadata().getName());

        EntandoBundleJobEntity installCompletedJob = null;

        Optional<EntandoBundleJobEntity> optionalExistingJob = getExistingJob(bundle);
        if (optionalExistingJob.isPresent()) {
            EntandoBundleJobEntity j = optionalExistingJob.get();
            JobStatus js = j.getStatus();
            if (js.equals(JobStatus.INSTALL_COMPLETED)) {
                installCompletedJob = j;
            }
            if (js.isOfType(JobType.UNFINISHED)) {
                throw new JobConflictException("Conflict with another job for the component " + j.getComponentId()
                        + " - JOB ID: " + j.getId());
            }
        }
        return Optional.ofNullable(installCompletedJob);
    }

    private Optional<EntandoBundleJobEntity> getExistingJob(EntandoDeBundle bundle) {
        String componentId = bundle.getMetadata().getName();
        Optional<EntandoBundleJobEntity> lastJobStarted = jobRepository
                .findFirstByComponentIdOrderByStartedAtDesc(componentId);
        if (lastJobStarted.isPresent()) {
            // To be an existing job it should be in a Running state
            if (JobStatus.getCompletedStatuses().contains(lastJobStarted.get().getStatus())) {
                return Optional.empty();
            }
            return lastJobStarted;
        }
        return Optional.empty();
    }

}
