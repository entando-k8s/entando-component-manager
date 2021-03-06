package org.entando.kubernetes.controller.digitalexchange.job;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.entando.kubernetes.model.job.EntandoBundleJob;
import org.entando.kubernetes.model.web.request.PagedListRequest;
import org.entando.kubernetes.model.web.response.PagedRestResponse;
import org.entando.kubernetes.model.web.response.SimpleRestResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/jobs")
public interface EntandoBundleJobResource {

    @Operation(description = "Get all jobs")
    @ApiResponse(responseCode = "200", description = "OK")
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<PagedRestResponse<EntandoBundleJob>> getAllJobs(PagedListRequest request);

    @Operation(description = "Get job by id")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(responseCode = "404", description = "Not Found")
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    SimpleRestResponse<EntandoBundleJob> getJob(@PathVariable("id") String id);

}
