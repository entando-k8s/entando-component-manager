package org.entando.kubernetes.service.digitalexchange.job;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.model.digitalexchange.EntandoBundleJob;
import org.entando.kubernetes.model.digitalexchange.JobStatus;
import org.entando.kubernetes.model.digitalexchange.JobType;
import org.entando.kubernetes.repository.EntandoBundleJobRepository;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EntandoBundleJobService {

    private final @NonNull EntandoBundleJobRepository jobRepository;

    public List<EntandoBundleJob> getAllJobs() {
        return jobRepository.findAllByOrderByStartedAtDesc();
    }

    public List<EntandoBundleJob> getAllJobs(String componentId) {
        return jobRepository.findAllByComponentIdOrderByStartedAtDesc(componentId);
    }

    public Optional<EntandoBundleJob> getById(String jobId) {
        return jobRepository.findById(UUID.fromString(jobId));
    }

    public Optional<EntandoBundleJob> getComponentLastJobOfType(String componentId, JobType type) {
        return jobRepository.findFirstByComponentIdAndStatusInOrderByStartedAtDesc(componentId, type.getStatusList());
    }

    public Optional<EntandoBundleJob> getComponentLastJobWithStatus(String componentId, JobStatus status) {
        return jobRepository.findFirstByComponentIdAndStatusOrderByStartedAtDesc(componentId, status);
    }
}
