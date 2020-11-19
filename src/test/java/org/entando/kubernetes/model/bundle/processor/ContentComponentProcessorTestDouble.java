package org.entando.kubernetes.model.bundle.processor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.entando.kubernetes.controller.digitalexchange.job.model.AnalysisReport;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallActionsByComponentType;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallRequest.InstallAction;
import org.entando.kubernetes.model.bundle.ComponentType;
import org.entando.kubernetes.model.bundle.descriptor.ComponentSpecDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.content.ContentDescriptor;
import org.entando.kubernetes.model.bundle.installable.Installable;
import org.entando.kubernetes.model.bundle.reader.BundleReader;
import org.entando.kubernetes.model.job.EntandoBundleComponentJobEntity;

class ContentComponentProcessorTestDouble implements ComponentProcessor<ContentDescriptor> {

    @Override
    public Class<ContentDescriptor> getDescriptorClass() {
        return ContentDescriptor.class;
    }

    @Override
    public Optional<Function<ComponentSpecDescriptor, List<String>>> getComponentSelectionFn() {
        return Optional.of(ComponentSpecDescriptor::getContents);
    }

    @Override
    public List<Installable<ContentDescriptor>> process(BundleReader bundleReader) {
        return new ArrayList<>();
    }

    @Override
    public List<Installable<ContentDescriptor>> process(BundleReader bundleReader, InstallAction conflictStrategy,
            InstallActionsByComponentType actions, AnalysisReport report) {
        return new ArrayList<>();
    }

    @Override
    public List<Installable> process(List components) {
        return new ArrayList<>();
    }

    @Override
    public ContentDescriptor buildDescriptorFromComponentJob(EntandoBundleComponentJobEntity component) {
        return new ContentDescriptor();
    }

    @Override
    public ComponentType getSupportedComponentType() {
        return ComponentType.CONTENT;
    }

}