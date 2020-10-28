package org.entando.kubernetes.model.bundle.processor;

import static java.util.Optional.ofNullable;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.client.core.EntandoCoreClient;
import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.model.bundle.ComponentType;
import org.entando.kubernetes.model.bundle.descriptor.AssetDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.BundleDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.ComponentSpecDescriptor;
import org.entando.kubernetes.model.bundle.installable.AssetInstallable;
import org.entando.kubernetes.model.bundle.installable.Installable;
import org.entando.kubernetes.model.bundle.reader.BundleReader;
import org.entando.kubernetes.model.job.EntandoBundleComponentJobEntity;
import org.springframework.stereotype.Service;

/**
 * Processor to handle Static files to be stored by Entando. Commonly used for js, images and css.
 *
 * <p>This processor will also create the folders.
 *
 * @author Sergio Marcelino
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssetProcessor implements ComponentProcessor<AssetDescriptor> {

    private final EntandoCoreClient engineService;

    @Override
    public ComponentType getSupportedComponentType() {
        return ComponentType.ASSET;
    }

    @Override
    public List<Installable<AssetDescriptor>> process(BundleReader npr) {
        try {
            BundleDescriptor descriptor = npr.readBundleDescriptor();
            List<String> assetDescriptors = ofNullable(descriptor.getComponents())
                    .map(ComponentSpecDescriptor::getAssets)
                    .orElse(Collections.emptyList());

            List<Installable<AssetDescriptor>> installables = new LinkedList<>();

            for (String fileName : assetDescriptors) {
                AssetDescriptor assetDescriptor = npr.readDescriptorFile(fileName, AssetDescriptor.class);
                installables.add(new AssetInstallable(engineService, assetDescriptor, npr.getAssetFile(
                        assetDescriptor.getCorrelationCode(), assetDescriptor.getName())));
            }

            return installables;
        } catch (IOException e) {
            throw new EntandoComponentManagerException("Error reading bundle", e);
        }
    }

    @Override
    public List<Installable<AssetDescriptor>> process(List<EntandoBundleComponentJobEntity> components) {
        return components.stream()
                .filter(c -> c.getComponentType() == ComponentType.ASSET)
                .map(c ->  new AssetInstallable(engineService, this.buildDescriptorFromComponentJob(c)))
                .collect(Collectors.toList());
    }

    @Override
    public AssetDescriptor buildDescriptorFromComponentJob(EntandoBundleComponentJobEntity component) {
        return AssetDescriptor.builder()
                .correlationCode(component.getComponentId())
                .build();
    }
}
