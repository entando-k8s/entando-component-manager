package org.entando.kubernetes.controller.digitalexchange.job;

import java.util.List;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.entando.kubernetes.model.digitalexchange.EntandoBundleJob;
import org.entando.kubernetes.model.digitalexchange.JobStatus;
import org.entando.kubernetes.model.digitalexchange.JobType;
import org.entando.kubernetes.model.web.response.PagedMetadata;
import org.entando.kubernetes.model.web.response.PagedRestResponse;
import org.entando.kubernetes.model.web.response.SimpleRestResponse;
import org.entando.kubernetes.service.digitalexchange.job.EntandoBundleJobService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.zalando.problem.Problem;
import org.zalando.problem.Status;
import org.zalando.problem.ThrowableProblem;

@RestController
@RequiredArgsConstructor
public class EntandoBundleJobResourceController implements EntandoBundleJobResource {

    private final @NonNull EntandoBundleJobService jobService;

    @Override
    public ResponseEntity<PagedRestResponse<EntandoBundleJob>> getAllJobs() {
        List<EntandoBundleJob> allJobs = jobService.getAllJobs();
        PagedMetadata<EntandoBundleJob> pagedMetadata = new PagedMetadata<>();
        pagedMetadata.setBody(allJobs);
        PagedRestResponse<EntandoBundleJob> response = new PagedRestResponse(pagedMetadata);
        return ResponseEntity.ok(response);
    }

    @Override
    public SimpleRestResponse<EntandoBundleJob> getJob(String jobId) {
        EntandoBundleJob job = jobService.getById(jobId).orElseThrow(() ->
                getNotFoundJobProblem(jobId));
        return new SimpleRestResponse(job);
    }

    @Override
    public ResponseEntity<PagedRestResponse<EntandoBundleJob>> getComponentJobs(@PathVariable("component") String componentId) {
            List<EntandoBundleJob> componentJobs = jobService.getAllJobs(componentId);
            PagedMetadata<EntandoBundleJob> pagedMetadata = new PagedMetadata<>();
            pagedMetadata.setBody(componentJobs);
            PagedRestResponse<EntandoBundleJob> response = new PagedRestResponse(pagedMetadata);
            return ResponseEntity.ok(response);
    }

    @Override
    public SimpleRestResponse<EntandoBundleJob> getLastComponentJobOfType(String componentId, JobType type) {
        EntandoBundleJob lastJobOfType = jobService.getComponentLastJobOfType(componentId, type)
                .orElseThrow(this::getGenericNotFoundJobProblem);
        return new SimpleRestResponse<>(lastJobOfType);
    }

    @Override
    public SimpleRestResponse<EntandoBundleJob> getLastComponentJobWithStatus(String componentId, JobStatus type) {
        EntandoBundleJob lastJobOfType = jobService.getComponentLastJobWithStatus(componentId, type)
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
