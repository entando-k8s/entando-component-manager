package org.entando.kubernetes.controller;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.model.EntandoPluginDeploymentResponse;
import org.entando.kubernetes.service.KubernetesService;
import org.entando.web.response.EntandoEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

@Slf4j
@RestController
@RequestMapping("/plugin")
@RequiredArgsConstructor
public class PluginController {

    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;

    private final @NonNull KubernetesService kubernetesService;

    @GetMapping(path = "", produces = JSON)
    public EntandoEntity<List<EntandoPluginDeploymentResponse>> list()  {
        log.info("Listing all deployed plugins");
        final List<EntandoPluginDeploymentResponse> list = kubernetesService.getDeployments();
        final EntandoEntity<List<EntandoPluginDeploymentResponse>> entity = new EntandoEntity<>();
        entity.setPayload(list);
        list.forEach(this::applyRel);
        return entity;
    }

    @GetMapping(path = "/{plugin}", produces = JSON)
    public EntandoEntity<EntandoPluginDeploymentResponse> get(@PathVariable final String plugin)  {
        log.info("Requesting plugin with identifier {}", plugin);
        return toResponse(kubernetesService.getDeployment(plugin));
    }

    private EntandoEntity<EntandoPluginDeploymentResponse> toResponse(final EntandoPluginDeploymentResponse response) {
        final EntandoEntity<EntandoPluginDeploymentResponse> entity = new EntandoEntity<>();
        entity.setPayload(response);
        entity.addMetadata("plugin", response.getPlugin());
        applyRel(response);
        return entity;
    }

    private void applyRel(final EntandoPluginDeploymentResponse response) {
        response.add(linkTo(methodOn(getClass()).get(response.getPlugin())).withSelfRel());
//        response.add(linkTo(methodOn(getClass()).get(response.getPlugin())).withRel("updateReplica"));
    }

}
