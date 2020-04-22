package org.entando.kubernetes.service.digitalexchange.job;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.model.digitalexchange.EntandoBundleComponentJob;
import org.entando.kubernetes.model.digitalexchange.EntandoBundleJob;
import org.entando.kubernetes.model.digitalexchange.JobStatus;
import org.entando.kubernetes.model.digitalexchange.JobType;
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

    public PagedMetadata<EntandoBundleJob> getJobs(PagedListRequest request) {
        List<EntandoBundleJob> allJobs = getJobs();
        List<EntandoBundleJob> filteredList = new EntandoBundleJobListProcessor(request, allJobs)
                .filterAndSort().toList();
        List<EntandoBundleJob> sublist = request.getSublist(filteredList);

        PagedMetadata<EntandoBundleJob> result = new PagedMetadata<>();
        result.setBody(sublist);
        result.setTotalItems(filteredList.size());
        result.setPage(request.getPage());
        result.setPageSize(request.getPageSize());
        return result;
    }

    public List<EntandoBundleComponentJob> getJobRelatedComponentJobs(EntandoBundleJob job) {
        return componentJobRepository.findAllByJob(job);
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
