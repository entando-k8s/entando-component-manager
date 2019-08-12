package org.entando.kubernetes.controller.digitalexchange.rating;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.entando.kubernetes.controller.Roles;
import org.entando.kubernetes.service.digitalexchange.rating.DERatingsSummary;
import org.entando.web.response.SimpleRestResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.validation.Valid;

@Api(tags = {"digital-exchange", "installation"})
@RequestMapping(value = "/components/rate")
public interface DEComponentRatingResource {

    @Secured(Roles.RATE)
    @ApiOperation(value = "Rate a component")
    @ApiResponses(@ApiResponse(code = 201, message = "Created"))
    @PostMapping(value = "/{exchange}/{component}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<SimpleRestResponse<DERatingsSummary>> rateComponent(@PathVariable("exchange") String exchangeId,
                                                                       @PathVariable("component") String componentId,
                                                                       @Valid @RequestBody DERatingValue rating);

}
