package org.entando.kubernetes.service.digitalexchange.job;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.model.job.EntandoBundleComponentJobEntity;
import org.entando.kubernetes.model.job.EntandoBundleJobEntity;
import org.entando.kubernetes.model.job.JobStatus;
import org.entando.kubernetes.model.job.JobType;
import org.entando.kubernetes.model.web.request.PagedListRequest;
import org.entando.kubernetes.model.web.response.PagedMetadata;
import org.entando.kubernetes.repository.EntandoBundleComponentJobRepository;
import org.entando.kubernetes.repository.EntandoBundleJobRepository;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EntandoBundleJobService {

    private final @NonNull EntandoBundleJobRepository jobRepository;
    private final @NonNull EntandoBundleComponentJobRepository componentJobRepository;

    public PagedMetadata<EntandoBundleJobEntity> getJobs(PagedListRequest request) {
        List<EntandoBundleJobEntity> allJobs = getJobs();
        List<EntandoBundleJobEntity> filteredList = new EntandoBundleJobListProcessor(request, allJobs)
                .filterAndSort().toList();
        List<EntandoBundleJobEntity> sublist = request.getSublist(filteredList);

        return new PagedMetadata<>(request, sublist, filteredList.size());
    }

    public List<EntandoBundleComponentJobEntity> getJobRelatedComponentJobs(EntandoBundleJobEntity job) {
        return componentJobRepository.findAllByParentJob(job);
    }

    public List<EntandoBundleJobEntity> getJobs() {
        return jobRepository.findAllByOrderByStartedAtDesc();
    }

    public List<EntandoBundleJobEntity> getJobs(String componentId) {
        return jobRepository.findAllByComponentIdOrderByStartedAtDesc(componentId);
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
}
