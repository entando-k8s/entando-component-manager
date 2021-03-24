package org.entando.kubernetes.controller.digitalexchange.job.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@JsonInclude(Include.NON_NULL)
public class ComponentInstallPlan {

    private Status status;
    @Default
    private LocalDateTime updateTime = null;
    @Default
    private String hash = null;
    @Default
    private InstallAction action = null;
}
