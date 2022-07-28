package org.entando.kubernetes.model.bundle.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.client.core.EntandoCoreClient;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallAction;
import org.entando.kubernetes.model.bundle.ComponentType;
import org.entando.kubernetes.model.bundle.descriptor.PageDescriptor;
import org.entando.kubernetes.model.bundle.installable.Installable;
import org.entando.kubernetes.model.bundle.installable.PageInstallable;
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
    
}
