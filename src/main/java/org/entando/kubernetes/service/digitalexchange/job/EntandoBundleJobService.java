package org.entando.kubernetes.service.digitalexchange.job;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.model.job.EntandoBundleComponentJob;
import org.entando.kubernetes.model.job.EntandoBundleJob;
import org.entando.kubernetes.model.job.JobStatus;
import org.entando.kubernetes.model.job.JobType;
import org.entando.kubernetes.model.web.request.PagedListRequest;
import org.entando.kubernetes.model.web.response.PagedMetadata;
import org.entando.kubernetes.repository.EntandoBundleComponentJobRepository;
import org.entando.kubernetes.repository.EntandoBundleJobRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EntandoBundleJobService {

    private final @NonNull EntandoBundleJobRepository jobRepository;
    private final @NonNull EntandoBundleComponentJobRepository componentJobRepository;

    public PagedMetadata<EntandoBundleJob> getJobs(PagedListRequest request) {
        List<EntandoBundleJob> allJobs = getJobs();
        List<EntandoBundleJob> filteredList = new EntandoBundleJobListProcessor(request, allJobs)
                .filterAndSort().toList();
        List<EntandoBundleJob> sublist = request.getSublist(filteredList);

        return new PagedMetadata<>(request, sublist, filteredList.size());
    }

    public List<EntandoBundleComponentJob> getJobRelatedComponentJobs(EntandoBundleJob job) {
        return componentJobRepository.findAllByParentJob(job);
    }

    public List<EntandoBundleJob> getJobs() {
        return jobRepository.findAllByOrderByStartedAtDesc();
    }

    public List<EntandoBundleJob> getJobs(String componentId) {
        return jobRepository.findAllByComponentIdOrderByStartedAtDesc(componentId);
    }

    public Optional<EntandoBundleJob> getById(String jobId) {
        return jobRepository.findById(UUID.fromString(jobId));
    }

    public Optional<EntandoBundleJob> getComponentLastJobOfType(String componentId, JobType type) {
        return jobRepository.findFirstByComponentIdAndStatusInOrderByStartedAtDesc(componentId, type.getStatuses());
    }

    public Optional<EntandoBundleJob> getComponentLastJobWithStatus(String componentId, JobStatus status) {
        return jobRepository.findFirstByComponentIdAndStatusOrderByStartedAtDesc(componentId, status);
    }
}
