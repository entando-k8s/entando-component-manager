package org.entando.kubernetes.service.digitalexchange.job.processors;

import org.entando.kubernetes.service.digitalexchange.job.ZipReader;
import org.entando.kubernetes.service.digitalexchange.job.model.WidgetDescriptor;
import org.entando.kubernetes.service.digitalexchange.model.DigitalExchange;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class WidgetComponentProcessor implements ComponentProcessor<WidgetDescriptor> {

    @Override
    public void processComponent(final DigitalExchange digitalExchange, final String componentId,
                                 final WidgetDescriptor descriptor, final ZipReader zipReader) throws IOException {
        if (descriptor.getCustomUiPath() != null) {
            zipReader.readFileAsString(descriptor.getCustomUiPath())
                    .ifPresent(descriptor::setCustomUi);
        }
    }

}
