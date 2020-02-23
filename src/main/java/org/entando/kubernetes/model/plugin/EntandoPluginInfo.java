package org.entando.kubernetes.model.plugin;

import javax.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
public class EntandoPluginInfo {

    @NotNull
    private String id;

    @NotNull
    private String name;

    private String description;

}
