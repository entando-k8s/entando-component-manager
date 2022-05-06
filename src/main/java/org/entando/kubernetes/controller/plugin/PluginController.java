package org.entando.kubernetes.controller.plugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.model.plugin.EntandoPlugin;
import org.entando.kubernetes.model.plugin.EntandoPluginInfo;
import org.entando.kubernetes.model.web.response.SimpleRestResponse;
import org.entando.kubernetes.service.KubernetesService;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.function.EntityResponse;

@Slf4j
@RestController
public class PluginController implements PluginResource {

    private final KubernetesService kubernetesService;

    public PluginController(KubernetesService kubernetesService) {
        this.kubernetesService = kubernetesService;
    }

    @Override
    public SimpleRestResponse<List<EntandoPlugin>> listLinkedPlugin() {
        log.info("Listing all deployed plugins");
        final List<EntandoPlugin> list = kubernetesService.getLinkedPlugins();
        final SimpleRestResponse<List<EntandoPlugin>> entity = new SimpleRestResponse<>();
        entity.setPayload(list);
        return entity;
    }

    @Override
    public ResponseEntity<CollectionModel<EntityModel<EntandoPlugin>>> listPlugin() {
        log.info("Listing all available plugins");
        final Collection<EntityModel<EntandoPlugin>> list = kubernetesService.getAllPlugins();
        final CollectionModel<EntityModel<EntandoPlugin>> collection = CollectionModel.of(new ArrayList<>(list));
        return ResponseEntity.ok(collection);
    }

    @Override
    public SimpleRestResponse<List<EntandoPluginInfo>> listLinkedPluginInfo() {
        log.info("Listing info about all linked plugins");
        final List<EntandoPlugin> linkedPlugins = kubernetesService.getLinkedPlugins();

        List<EntandoPluginInfo> payload = linkedPlugins.stream()
                .map(this::convertToEntandoPluginInfo)
                .collect(Collectors.toList());

        return new SimpleRestResponse<>(payload);
    }

    @Override
    public SimpleRestResponse<EntandoPlugin> get(@PathVariable final String pluginId) {
        log.info("Requesting plugin with identifier {}", pluginId);
        return new SimpleRestResponse<>(kubernetesService.getLinkedPlugin(pluginId));
    }

    private EntandoPluginInfo convertToEntandoPluginInfo(EntandoPlugin entandoPlugin) {
        EntandoPluginInfo result = new EntandoPluginInfo();

        result.setName(entandoPlugin.getMetadata().getName());
        result.setId(entandoPlugin.getMetadata().getUid());
        if (entandoPlugin.getMetadata().getAnnotations() != null) {
            result.setDescription(entandoPlugin.getMetadata().getAnnotations().get("description"));
        }

        return result;
    }


}
