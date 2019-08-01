package org.entando.kubernetes.service.digitalexchange.job.processors;

import org.entando.kubernetes.service.digitalexchange.job.ZipReader;
import org.entando.kubernetes.service.digitalexchange.job.model.PageModelDescriptor;
import org.entando.kubernetes.service.digitalexchange.job.model.WidgetDescriptor;
import org.entando.kubernetes.service.digitalexchange.model.DigitalExchange;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class PageModelComponentProcessor implements ComponentProcessor<PageModelDescriptor> {

    @Override
    public void processComponent(final DigitalExchange digitalExchange, final String componentId,
                                 final PageModelDescriptor descriptor, final ZipReader zipReader) throws IOException {
        if (descriptor.getTemplatePath() != null) {
            zipReader.readFileAsString(descriptor.getTemplatePath())
                    .ifPresent(descriptor::setTemplate);
        }
    }
}
