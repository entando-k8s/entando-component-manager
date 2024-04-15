package org.entando.kubernetes.model.bundle.processor;

import static org.entando.kubernetes.service.digitalexchange.BundleUtilities.removeProtocolAndGetBundleId;

import java.io.IOException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.client.core.EntandoCoreClient;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallAction;
import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.model.bundle.ComponentType;
import org.entando.kubernetes.model.bundle.descriptor.PageDescriptor;
import org.entando.kubernetes.model.bundle.installable.Installable;
import org.entando.kubernetes.model.bundle.installable.PageInstallable;
import org.entando.kubernetes.model.bundle.reader.BundleReader;
import org.entando.kubernetes.model.bundle.reportable.EntandoEngineReportableProcessor;
import org.entando.kubernetes.validator.descriptor.PageDescriptorValidator;
import org.springframework.stereotype.Service;

/**
 * Processor to handle Pages.
 */
@Slf4j
@Service
public class PageProcessor extends AbstractPageProcessor implements EntandoEngineReportableProcessor {

    public PageProcessor(EntandoCoreClient engineService, PageDescriptorValidator descriptorValidator) {
        super(engineService, descriptorValidator);
    }

    @Override
    public ComponentType getSupportedComponentType() {
        return ComponentType.PAGE;
    }

    @Override
    protected Installable<PageDescriptor> getInstallable(PageDescriptor pageDescriptor, InstallAction action) {
        return new PageInstallable(this.getEngineService(), pageDescriptor, action);
    }

    // TODO remove this method when we will add the composeAndSetCode(descriptor, bundleReader) instruction in the parent method
    @Override
    public List<String> readDescriptorKeys(BundleReader bundleReader, String fileName,
            ComponentProcessor<?> componentProcessor) {

        final String bundleId = removeProtocolAndGetBundleId(bundleReader.getBundleUrl());

        try {
            PageDescriptor descriptor = (PageDescriptor) bundleReader.readDescriptorFile(fileName,
                    componentProcessor.getDescriptorClass());
            composeAndSetCode(descriptor, bundleReader);
            return List.of(ProcessorHelper.replaceBundleIdPlaceholder(descriptor.getComponentKey().getKey(), bundleId));
        } catch (IOException e) {
            throw new EntandoComponentManagerException(String.format(
                    "Error parsing content type %s from descriptor %s",
                    componentProcessor.getSupportedComponentType(), fileName), e);
        }
    }
}
