package org.entando.kubernetes.controller.digitalexchange.job;

import java.util.List;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.entando.kubernetes.model.job.EntandoBundleComponentJob;
import org.entando.kubernetes.model.job.EntandoBundleJob;
import org.entando.kubernetes.model.job.EntandoBundleJobDto;
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
    public ResponseEntity<PagedRestResponse<EntandoBundleJobDto>> getAllJobs(PagedListRequest request) {
        PagedMetadata<EntandoBundleJob> tempJobs = jobService.getJobs(request);
        List<EntandoBundleJobDto> jobDtos = tempJobs.getBody().stream()
                .map(j -> EntandoBundleJobDto.from(j, jobService.getJobRelatedComponentJobs(j)))
                .collect(Collectors.toList());
        PagedMetadata<EntandoBundleJobDto> pagedMetadata = new PagedMetadata<>(request, jobDtos,
                tempJobs.getTotalItems());
        return ResponseEntity.ok(new PagedRestResponse<>(pagedMetadata));
    }

    @Override
    public SimpleRestResponse<EntandoBundleJobDto> getJob(@PathVariable("id") String id) {
        EntandoBundleJob job = jobService.getById(id).orElseThrow(() ->
                getNotFoundJobProblem(id));
        List<EntandoBundleComponentJob> jobRelatedComponentJobs = jobService.getJobRelatedComponentJobs(job);
        EntandoBundleJobDto jobDto = EntandoBundleJobDto.from(job, jobRelatedComponentJobs);
        return new SimpleRestResponse<>(jobDto);
    }

    private ThrowableProblem getNotFoundJobProblem(String jobId) {
        return Problem.valueOf(Status.NOT_FOUND, "A job with id " + jobId + " could not be found");
    }

}
