package org.entando.kubernetes.model.bundle.installable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.model.DbmsVendor;
import org.entando.kubernetes.model.bundle.descriptor.DockerImage;
import org.entando.kubernetes.model.bundle.descriptor.PluginDescriptor;
import org.entando.kubernetes.model.digitalexchange.ComponentType;
import org.entando.kubernetes.model.plugin.EntandoPlugin;
import org.entando.kubernetes.model.plugin.EntandoPluginBuilder;
import org.entando.kubernetes.model.plugin.ExpectedRole;
import org.entando.kubernetes.service.KubernetesService;

@Slf4j
public class PluginInstallable extends Installable<PluginDescriptor> {

    private final KubernetesService kubernetesService;

    public PluginInstallable(KubernetesService kubernetesService, PluginDescriptor plugin) {
        super(plugin);
        this.kubernetesService = kubernetesService;
    }

    @Override
    public CompletableFuture<Void> install() {
        return CompletableFuture.runAsync(() -> {
            log.info("Deploying plugin {}", getName());
            EntandoPlugin plugin = generateFromDescriptor(representation);
            kubernetesService.linkPluginAndWaitForSuccess(plugin);
        });
    }

    private EntandoPlugin generateFromDescriptor(PluginDescriptor descriptor) {
        return new EntandoPluginBuilder()
                .withNewMetadata()
                    .withName(getNameFromImage(descriptor.getDockerImage()))
                    .withLabels(getLabelsFromImage(descriptor.getDockerImage()))
                .endMetadata()
                .withNewSpec()
                    .withDbms(DbmsVendor.valueOf(descriptor.getDbms().toUpperCase()))
                    .withImage(descriptor.getImage())
                    .withIngressPath(getIngressPathFromImage(descriptor.getDockerImage()))
                    .withRoles(getRolesFromDescriptor(descriptor))
                    .withHealthCheckPath(descriptor.getHealthCheckPath())
                .endSpec()
                .build();
    }

    private List<ExpectedRole> getRolesFromDescriptor(PluginDescriptor descriptor) {
        return descriptor.getRoles().stream()
                .distinct()
                .map(ExpectedRole::new)
                .collect(Collectors.toList());
    }

    private String getNameFromImage(DockerImage image) {
        return String.join(".",
                makeNameCompatible(image.getOrganization()),
                makeNameCompatible(image.getName()),
                makeNameCompatible(image.getVersion()));
    }

    private String getIngressPathFromImage(DockerImage image) {
        return "/" + String.join("/",
                makeNameCompatible(image.getOrganization()),
                makeNameCompatible(image.getName()),
                makeNameCompatible(image.getVersion()));
    }

    private Map<String, String> getLabelsFromImage(DockerImage dockerImage) {
        Map<String, String> labels = new HashMap<>();
        labels.put("organization", dockerImage.getOrganization());
        labels.put("name", dockerImage.getName());
        labels.put("version", dockerImage.getVersion());
        return labels;
    }

    private String makeNameCompatible(String value) {
        value = value.toLowerCase();
        value = value.replaceAll("_", ".");
        return value;
    }

    @Override
    public CompletableFuture<Void> uninstall() {
        return CompletableFuture.runAsync(() -> {
            log.info("Removing link to plugin {}", getName());
            DockerImage image = DockerImage.fromString(getName());
            kubernetesService.unlinkPlugin(getNameFromImage(image));
        });
    }

    @Override
    public ComponentType getComponentType() {
        return ComponentType.PLUGIN;
    }

    @Override
    public String getName() {
        return this.representation.getDockerImage().toString();
    }

}
