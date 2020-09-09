package org.entando.kubernetes.model.bundle.installable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.model.DbmsVendor;
import org.entando.kubernetes.model.bundle.descriptor.PluginDescriptor;
import org.entando.kubernetes.model.digitalexchange.ComponentType;
import org.entando.kubernetes.model.plugin.EntandoPlugin;
import org.entando.kubernetes.model.plugin.EntandoPluginBuilder;
import org.entando.kubernetes.model.plugin.EntandoPluginSpecBuilder;
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
                    .withGenerateName(getNameFromDescriptor(descriptor))
                    .withLabels(getLabelsFromDescriptor(descriptor))
                .endMetadata()
                .withNewSpec()
                    .withDbms(DbmsVendor.valueOf(descriptor.getDbms()))
                    .withImage(descriptor.getImage())
                    .withIngressPath(getIngressPathFromDescriptor(descriptor))
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

    private String getNameFromDescriptor(PluginDescriptor descriptor) {
        String image = descriptor.getImage();
        Pattern p = Pattern.compile("(?<organization>\\w+)/(?<name>\\w+):(?<version>.*)");
        Matcher m = p.matcher(image);
        if (!m.find()) {
            throw new RuntimeException("Impossible to generate a name from the image value");
        }
        return String.join(".",
                makeNameCompatible(m.group("organization")),
                makeNameCompatible(m.group("name")),
                makeNameCompatible(m.group("version")));
    }

    private String getIngressPathFromDescriptor(PluginDescriptor descriptor) {
        String image = descriptor.getImage();
        Pattern p = Pattern.compile("(?<organization>\\w+)/(?<name>\\w+):(?<version>.*)");
        Matcher m = p.matcher(image);
        if (!m.find()) {
            throw new RuntimeException("Impossible to generate a name from the image value");
        }
        return "/" + String.join("/",
                makeNameCompatible(m.group("organization")),
                makeNameCompatible(m.group("name")),
                makeNameCompatible(m.group("version")));
    }

    private Map<String, String> getLabelsFromDescriptor(PluginDescriptor descriptor) {
        String image = descriptor.getImage();
        Pattern p = Pattern.compile("(?<organization>\\w+)/(?<name>\\w+):(?<version>.*)");
        Matcher m = p.matcher(image);
        if (!m.find()) {
            throw new RuntimeException("Impossible to generate a name from the image value");
        }
        Map<String, String> labels = new HashMap<>();
        labels.put("organization", m.group("organization"));
        labels.put("name", m.group("name"));
        labels.put("version", m.group("version"));
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
            kubernetesService.unlinkPlugin(getName());
        });
    }

    @Override
    public ComponentType getComponentType() {
        return ComponentType.PLUGIN;
    }

    @Override
    public String getName() {
        return this.representation.getImage();
    }

}
