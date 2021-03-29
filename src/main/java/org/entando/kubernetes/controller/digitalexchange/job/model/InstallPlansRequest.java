/**
 * request to start the install plans flow.
 * this request is used to request an install plans from the ECR
 */

package org.entando.kubernetes.controller.digitalexchange.job.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstallPlansRequest {

    @Default
    private final String version = "latest";

}
