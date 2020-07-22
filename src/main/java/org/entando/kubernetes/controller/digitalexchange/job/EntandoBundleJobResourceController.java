package org.entando.kubernetes.controller.digitalexchange.job;

import java.util.List;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.entando.kubernetes.model.bundle.EntandoBundleJob;
import org.entando.kubernetes.model.job.EntandoBundleComponentJobEntity;
import org.entando.kubernetes.model.job.EntandoBundleJobEntity;
import org.entando.kubernetes.model.web.request.PagedListRequest;
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
    public ResponseEntity<PagedRestResponse<EntandoBundleJob>> getAllJobs(PagedListRequest request) {
        PagedMetadata<EntandoBundleJobEntity> tempJobs = jobService.getJobs(request);
        List<EntandoBundleJob> jobDtos = tempJobs.getBody().stream()
                .map(j -> EntandoBundleJob.fromEntity(j, jobService.getJobRelatedComponentJobs(j)))
                .collect(Collectors.toList());
        PagedMetadata<EntandoBundleJob> pagedMetadata = new PagedMetadata<>(request, jobDtos,
                tempJobs.getTotalItems());
        return ResponseEntity.ok(new PagedRestResponse<>(pagedMetadata));
    }

    @Override
    public SimpleRestResponse<EntandoBundleJob> getJob(@PathVariable("id") String id) {
        EntandoBundleJobEntity job = jobService.getById(id).orElseThrow(() ->
                getNotFoundJobProblem(id));
        List<EntandoBundleComponentJobEntity> jobRelatedComponentJobs = jobService.getJobRelatedComponentJobs(job);
        EntandoBundleJob jobDto = EntandoBundleJob.fromEntity(job, jobRelatedComponentJobs);
        return new SimpleRestResponse<>(jobDto);
    }

    private ThrowableProblem getNotFoundJobProblem(String jobId) {
        return Problem.valueOf(Status.NOT_FOUND, "A job with id " + jobId + " could not be found");
    }

}
