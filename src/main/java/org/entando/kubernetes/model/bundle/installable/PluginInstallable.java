package org.entando.kubernetes.model.bundle.installable;

import java.util.HashSet;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.entando.kubernetes.client.k8ssvc.K8SServiceClient.PluginConfiguration;
import org.entando.kubernetes.config.tenant.thread.ContextCompletableFuture;
import org.entando.kubernetes.config.tenant.thread.TenantContextHolder;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallAction;
import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.model.bundle.ComponentType;
import org.entando.kubernetes.model.bundle.descriptor.plugin.PluginDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.plugin.PluginDescriptor.DescriptorMetadata;
import org.entando.kubernetes.model.job.PluginDataEntity;
import org.entando.kubernetes.model.plugin.EntandoPlugin;
import org.entando.kubernetes.repository.PluginDataRepository;
import org.entando.kubernetes.service.KubernetesService;
import org.entando.kubernetes.service.digitalexchange.BundleUtilities;

@Slf4j
public class PluginInstallable extends Installable<PluginDescriptor> {

    private final KubernetesService kubernetesService;
    private final PluginDataRepository pluginDataRepository;

    public PluginInstallable(KubernetesService kubernetesService, PluginDescriptor plugin, InstallAction action,
            PluginDataRepository pluginDataRepository) {
        super(plugin, action);
        this.kubernetesService = kubernetesService;
        this.pluginDataRepository = pluginDataRepository;
    }

    @Override
    public CompletableFuture<Void> install() {
        return ContextCompletableFuture.runAsyncWithContext(() -> {

            logConflictStrategyAction();

            EntandoPlugin plugin = buildPlugin(representation);

            if (shouldSkip()) {
                fixPluginRegistrationIfNecessary();
                return;
            }

            if (shouldCreate()) {
                installPlugin(plugin);
            } else if (shouldOverride()) {
                overridePlugin(plugin);
            } else {
                throw new EntandoComponentManagerException("Illegal state detected");
            }
        });
    }

    private EntandoPlugin buildPlugin(PluginDescriptor descriptor) {
        Optional<PluginConfiguration> configuration = kubernetesService
                .getPluginConfiguration(descriptor.getDescriptorMetadata().getPluginCode());
        return BundleUtilities.generatePluginFromDescriptor(descriptor, configuration);

    }

    private void installPlugin(EntandoPlugin plugin) {
        kubernetesService.linkPluginAndWaitForSuccess(plugin);

        PluginDataEntity pluginDataEntity = composePluginDataEntity(new PluginDataEntity());
        pluginDataRepository.save(pluginDataEntity);
    }

    private void overridePlugin(EntandoPlugin plugin) {
        kubernetesService.unlink(plugin.getMetadata().getName());
        kubernetesService.linkPluginAndWaitForSuccess(plugin);

        PluginDataEntity pluginDataEntity = pluginDataRepository.findByBundleIdAndPluginName(
                        representation.getDescriptorMetadata().getBundleId(),
                        representation.getDescriptorMetadata().getPluginName())
                .orElseGet(PluginDataEntity::new);
        pluginDataEntity = composePluginDataEntity(pluginDataEntity);
        pluginDataRepository.save(pluginDataEntity);
    }

    /**
     * Certain error conditions can lead to plugin present but not registered in the PluginDataRepository. In turn this
     * causes a query plan SKIP that is inconsistent with the status of the PluginDataRepository. Therefore, in case of
     * SKIP this function double-checks the actual presence of the plugin and in case it registers it on the
     * PluginDataRepository.
     */
    private void fixPluginRegistrationIfNecessary() {
        DescriptorMetadata meta = representation.getDescriptorMetadata();

        boolean pluginNotRecordedInCM = pluginDataRepository.findByBundleIdAndPluginName(
                meta.getBundleId(), meta.getPluginName()
        ).isEmpty();

        if (pluginNotRecordedInCM) {
            boolean pluginLinkedInK8S = kubernetesService.isPluginLinked(meta.getPluginCode());

            if (pluginLinkedInK8S) {
                logInconsistentStateWarning(meta);
                var pluginDataEntity = composePluginDataEntity(new PluginDataEntity());
                pluginDataRepository.save(pluginDataEntity);
            }
        }
    }

    private PluginDataEntity composePluginDataEntity(PluginDataEntity pluginData) {
        final DescriptorMetadata metadata = representation.getDescriptorMetadata();

        return pluginData.setBundleId(metadata.getBundleId())
                .setPluginName(metadata.getPluginName())
                .setPluginCode(metadata.getPluginCodeTenantAware())
                .setEndpoint(metadata.getEndpoint())
                .setCustomEndpoint(metadata.getCustomEndpoint())
                .setRoles(representation.getRoles() != null ? new HashSet<String>(representation.getRoles()) : null)
                .setDockerTag(representation.getDockerImage().getTag())
                .setDockerSha256(representation.getDockerImage().getSha256());
    }

    @Override
    public CompletableFuture<Void> uninstallFromEcr() {
        return ContextCompletableFuture.runAsyncWithContext(() -> {

            String pluginCode = representation.getComponentKey().getKey();

            if (shouldSkip()) {
                return; //Do nothing
            }

            log.info("Removing ingress path to plugin with code:'{}' from tenant {}", pluginCode,
                    TenantContextHolder.getCurrentTenantCode());
            kubernetesService.removeIngressPathForPlugin(pluginCode);

            log.info("Removing link to plugin {} from tenant {}", pluginCode,
                    TenantContextHolder.getCurrentTenantCode());
            kubernetesService.unlinkAndScaleDownPlugin(pluginCode);

            if (!deletePluginData(pluginCode)) {
                log.warn("Plugin uninstalled but no data has been deleted from plugin_api_data db table");
            }
        });
    }

    @Override
    public boolean shouldUninstallFromAppEngine() {
        log.debug("should delete:'false' element type:'PLUGIN' name:'{}'", getName());
        return false;
    }

    @Override
    public ComponentType getComponentType() {
        return ComponentType.PLUGIN;
    }

    @Override
    public String getName() {
        return this.representation.getDescriptorMetadata().getPluginCodeTenantAware();
    }

    /**
     * delete from the db the info about the plugin api data.
     *
     * @param pluginCode the plugin code from which recover the data required to execute the query
     * @return true if a record has been delete, false otherwise
     */
    private boolean deletePluginData(String pluginCode) {
        if (ObjectUtils.isEmpty(pluginCode)) {
            log.warn("Empty plugin code retrieved by the database. Can't delete plugin detail");
            return false;
        }

        return pluginDataRepository.deleteByPluginCode(pluginCode) > 0;
    }

    private static void logInconsistentStateWarning(DescriptorMetadata meta) {
        log.warn(
                "Inconsistent state detected for bundle {} / plugin {}: in K8S but not in CM",
                meta.getPluginId(), meta.getPluginName()
        );
    }
}
