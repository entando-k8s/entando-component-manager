package org.entando.kubernetes.controller.digitalexchange.job.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class InstallWithPlansRequest extends InstallPlan {

    private String version = "latest";
}
