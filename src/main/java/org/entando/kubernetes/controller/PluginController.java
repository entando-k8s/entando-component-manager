package org.entando.kubernetes.controller;

import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.model.link.EntandoAppPluginLink;
import org.entando.kubernetes.model.plugin.EntandoPlugin;
import org.entando.kubernetes.service.KubernetesService;
import org.entando.web.response.SimpleRestResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;

@Slf4j
@RestController
@RequestMapping("/plugins")
public class PluginController {

    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;

    private final KubernetesService kubernetesService;
    private final String entandoAppName;
    private final String entandoAppNamespace;

    public PluginController(KubernetesService kubernetesService,
            @Value("${entando.app.name}") String entandoAppName,
            @Value("${entando.app.namespace}") String entandoAppNamespace) {
        this.kubernetesService = kubernetesService;
        this.entandoAppName = entandoAppName;
        this.entandoAppNamespace = entandoAppNamespace;
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


    private void applyRel(final EntandoPlugin response) {
//        response.add(linkTo(methodOn(getClass()).get(response.getPlugin())).withSelfRel());
//        response.add(linkTo(methodOn(getClass()).get(response.getPlugin())).withRel("updateReplica"));
    }

}
