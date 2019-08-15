package org.entando.kubernetes.controller.digitalexchange.rating;

import lombok.Data;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;

@Data
@Validated
public class DEComponentRatingRequest {

    @NotBlank
    private String componentId;

    @Min(1)
    @Max(5)
    private int rating;

}
