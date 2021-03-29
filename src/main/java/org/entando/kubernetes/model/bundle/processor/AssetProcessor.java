package org.entando.kubernetes.model.bundle.processor;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.client.core.EntandoCoreClient;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallAction;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallPlan;
import org.entando.kubernetes.model.bundle.ComponentType;
import org.entando.kubernetes.model.bundle.descriptor.AssetDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.ComponentSpecDescriptor;
import org.entando.kubernetes.model.bundle.installable.AssetInstallable;
import org.entando.kubernetes.model.bundle.installable.Installable;
import org.entando.kubernetes.model.bundle.reader.BundleReader;
import org.entando.kubernetes.model.bundle.reportable.EntandoCMSReportableProcessor;
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
public class AssetProcessor extends BaseComponentProcessor<AssetDescriptor>
        implements EntandoCMSReportableProcessor {

    private final EntandoCoreClient engineService;

    @Override
    public ComponentType getSupportedComponentType() {
        return ComponentType.ASSET;
    }

    @Override
    public Class<AssetDescriptor> getDescriptorClass() {
        return AssetDescriptor.class;
    }

    @Override
    public Optional<Function<ComponentSpecDescriptor, List<String>>> getComponentSelectionFn() {
        return Optional.of(ComponentSpecDescriptor::getAssets);
    }


    @Override
    public List<Installable<AssetDescriptor>> process(BundleReader bundleReader) {
        return this.process(bundleReader, InstallAction.CREATE,
                new InstallPlan());
    }

    @Override
    public List<Installable<AssetDescriptor>> process(BundleReader bundleReader, InstallAction conflictStrategy,
            InstallPlan installPlan) {

        List<Installable<AssetDescriptor>> installables = new LinkedList<>();

        try {
            final List<String> descriptorList = getDescriptorList(bundleReader);

            for (String fileName : descriptorList) {
                String assetDirectory = Paths.get(fileName).getParent().toString();
                AssetDescriptor assetDescriptor = bundleReader.readDescriptorFile(fileName, AssetDescriptor.class);
                InstallAction action = extractInstallAction(assetDescriptor.getCorrelationCode(),
                        conflictStrategy, installPlan);
                installables.add(new AssetInstallable(engineService, assetDescriptor, bundleReader.getAssetFile(
                        assetDirectory, assetDescriptor.getName()), action));
            }

        } catch (IOException e) {
            throw makeMeaningfulException(e);
        }

        return installables;
    }

    @Override
    public List<Installable<AssetDescriptor>> process(List<EntandoBundleComponentJobEntity> components) {
        return components.stream()
                .filter(c -> c.getComponentType() == getSupportedComponentType())
                .map(c -> new AssetInstallable(engineService, this.buildDescriptorFromComponentJob(c), c.getAction()))
                .collect(Collectors.toList());
    }

    @Override
    public AssetDescriptor buildDescriptorFromComponentJob(EntandoBundleComponentJobEntity component) {
        return AssetDescriptor.builder()
                .correlationCode(component.getComponentId())
                .build();
    }
}
