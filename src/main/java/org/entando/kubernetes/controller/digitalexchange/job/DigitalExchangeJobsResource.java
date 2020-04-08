package org.entando.kubernetes.controller.digitalexchange.job;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.entando.kubernetes.model.digitalexchange.DigitalExchangeJob;
import org.entando.kubernetes.model.digitalexchange.JobStatus;
import org.entando.kubernetes.model.digitalexchange.JobType;
import org.entando.kubernetes.model.web.response.PagedRestResponse;
import org.entando.kubernetes.model.web.response.SimpleRestResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RequestMapping("/jobs")
public interface DigitalExchangeJobsResource {

    @Operation(description = "Get all jobs")
    @ApiResponse(responseCode = "200", description = "OK")
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<PagedRestResponse<DigitalExchangeJob>> getAllJobs();

    @Operation(description = "Get job by id")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(responseCode = "404", description = "Not Found")
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    SimpleRestResponse<DigitalExchangeJob> getJob(@PathVariable("id") String jobId);

    @Operation(description = "Get all jobs for a component")
    @ApiResponse(responseCode = "200", description = "OK")
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE, params = "component")
    ResponseEntity<PagedRestResponse<DigitalExchangeJob>> getComponentJobs(
            @RequestParam(value = "component", required = false) String componentId);

    @Operation(description = "Get last job of type for component")
    @ApiResponse(responseCode = "200", description = "OK")
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE, params = {"component", "type"})
    SimpleRestResponse<DigitalExchangeJob> getLastComponentJobOfType(@RequestParam("component") String componentId,
            @RequestParam(value = "type", required = false) JobType type);

    @Operation(description = "Get last job with status for component")
    @ApiResponse(responseCode = "200", description = "OK")
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE, params = {"component", "status"})
    SimpleRestResponse<DigitalExchangeJob> getLastComponentJobWithStatus(@RequestParam("component") String componentId,
            @RequestParam(value = "status", required = false) JobStatus type);
}
