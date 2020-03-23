package org.entando.kubernetes.controller.digitalexchange.job;

import java.util.List;
import java.util.Optional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.entando.kubernetes.model.digitalexchange.DigitalExchangeJob;
import org.entando.kubernetes.model.digitalexchange.JobStatus;
import org.entando.kubernetes.model.digitalexchange.JobType;
import org.entando.kubernetes.model.web.response.PagedMetadata;
import org.entando.kubernetes.model.web.response.PagedRestResponse;
import org.entando.kubernetes.model.web.response.SimpleRestResponse;
import org.entando.kubernetes.service.digitalexchange.job.DigitalExchangeJobService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.zalando.problem.Problem;
import org.zalando.problem.Status;
import org.zalando.problem.ThrowableProblem;
import sun.java2d.pipe.SpanShapeRenderer.Simple;

@RestController
@RequiredArgsConstructor
public class DigitalExchangeJobsResourceController implements DigitalExchangeJobsResource {

    private final @NonNull DigitalExchangeJobService jobService;

    @Override
    public ResponseEntity<PagedRestResponse<DigitalExchangeJob>> getAllJobs() {
        List<DigitalExchangeJob> allJobs = jobService.getAllJobs();
        PagedMetadata<DigitalExchangeJob> pagedMetadata = new PagedMetadata<>();
        pagedMetadata.setBody(allJobs);
        PagedRestResponse<DigitalExchangeJob> response = new PagedRestResponse(pagedMetadata);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<DigitalExchangeJob> getJob(String jobId) {
        DigitalExchangeJob job = jobService.getById(jobId).orElseThrow(() ->
                getNotFoundJobProblem(jobId));
        return ResponseEntity.ok(job);
    }


    @Override
    public ResponseEntity<PagedRestResponse<DigitalExchangeJob>> getComponentJobs(@PathVariable("component") String componentId) {
            List<DigitalExchangeJob> componentJobs = jobService.getAllJobs(componentId);
            PagedMetadata<DigitalExchangeJob> pagedMetadata = new PagedMetadata<>();
            pagedMetadata.setBody(componentJobs);
            PagedRestResponse<DigitalExchangeJob> response = new PagedRestResponse(pagedMetadata);
            return ResponseEntity.ok(response);
    }

    @Override
    public SimpleRestResponse<DigitalExchangeJob> getLastComponentJobOfType(String componentId, JobType type) {
        DigitalExchangeJob lastJobOfType = jobService.getComponentLastJobOfType(componentId, type)
                .orElseThrow(this::getGenericNotFoundJobProblem);
        return new SimpleRestResponse<>(lastJobOfType);
    }

    @Override
    public SimpleRestResponse<DigitalExchangeJob> getLastComponentJobWithStatus(String componentId, JobStatus type) {
        DigitalExchangeJob lastJobOfType = jobService.getComponentLastJobWithStatus(componentId, type)
                .orElseThrow(this::getGenericNotFoundJobProblem);
        return new SimpleRestResponse<>(lastJobOfType);
    }

    private ThrowableProblem getNotFoundJobProblem(String jobId) {
        return Problem.valueOf(Status.NOT_FOUND, "A job with id " + jobId + " could not be found");
    }

    private ThrowableProblem getGenericNotFoundJobProblem() {
        return Problem.valueOf(Status.NOT_FOUND );
    }
}
