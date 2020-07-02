package org.entando.kubernetes.model.plugin;

import lombok.Data;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotNull;
import java.util.Map;

@Data
@Validated
public class EntandoPluginInfo {

    @NotNull
    private String id;

    @NotNull
    private String name;

    private String description;

    private Map<String, Object> configUI;

}
