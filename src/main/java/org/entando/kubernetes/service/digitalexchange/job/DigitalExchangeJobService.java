package org.entando.kubernetes.service.digitalexchange.job;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.model.digitalexchange.DigitalExchangeJob;
import org.entando.kubernetes.model.digitalexchange.JobStatus;
import org.entando.kubernetes.model.digitalexchange.JobType;
import org.entando.kubernetes.repository.DigitalExchangeJobRepository;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DigitalExchangeJobService {

    private final @NonNull DigitalExchangeJobRepository jobRepository;

    public List<DigitalExchangeJob> getAllJobs() {
        return jobRepository.findAllByOrderByFinishedAtDesc();
    }

    public List<DigitalExchangeJob> getAllJobs(String componentId) {
        return jobRepository.findAllByComponentIdOrderByStartedAtDesc(componentId);
    }

    public Optional<DigitalExchangeJob> getById(String jobId) {
        return jobRepository.findById(UUID.fromString(jobId));
    }

    public Optional<DigitalExchangeJob> getComponentLastJobOfType(String componentId, JobType type) {
        return jobRepository.findFirstByComponentIdAndStatusInOrderByStartedAtDesc(componentId, type.getStatusList());
    }

    public Optional<DigitalExchangeJob> getComponentLastJobWithStatus(String componentId, JobStatus status) {
        return jobRepository.findFirstByComponentIdAndStatusOrderByStartedAtDesc(componentId, status);
    }
}
