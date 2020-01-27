package org.entando.kubernetes.controller;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.model.plugin.EntandoPlugin;
import org.entando.kubernetes.service.KubernetesService;
import org.entando.kubernetes.model.web.response.SimpleRestResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/plugins")
public class PluginController {

    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;

    private final KubernetesService kubernetesService;

    public PluginController(KubernetesService kubernetesService){
        this.kubernetesService = kubernetesService;
    }

    @GetMapping(path = "", produces = JSON)
    public SimpleRestResponse<List<EntandoPlugin>> listLinkedPlugin()  {
        log.info("Listing all deployed plugins");
        final List<EntandoPlugin> list = kubernetesService.getLinkedPlugins();
        final SimpleRestResponse<List<EntandoPlugin>> entity = new SimpleRestResponse<>();
        entity.setPayload(list);
        return entity;
    }

    @GetMapping(path = "/{pluginId}", produces = JSON)
    public SimpleRestResponse<EntandoPlugin> get(@PathVariable final String pluginId)  {
        log.info("Requesting plugin with identifier {}", pluginId);
        return new SimpleRestResponse<>(kubernetesService.getLinkedPlugin(pluginId));
    }


}
