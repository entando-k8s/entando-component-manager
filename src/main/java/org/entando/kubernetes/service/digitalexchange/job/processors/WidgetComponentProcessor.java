package org.entando.kubernetes.service.digitalexchange.job.processors;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.service.digitalexchange.entandocore.EntandoEngineService;
import org.entando.kubernetes.service.digitalexchange.job.ZipReader;
import org.entando.kubernetes.service.digitalexchange.job.model.WidgetDescriptor;
import org.entando.kubernetes.service.digitalexchange.model.DigitalExchange;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
public class WidgetComponentProcessor implements ComponentProcessor<WidgetDescriptor> {

    private final @NonNull EntandoEngineService engineService;

    @Override
    public void processComponent(final DigitalExchange digitalExchange, final String componentId,
                                 final WidgetDescriptor descriptor, final ZipReader zipReader,
                                 final String folder) throws IOException {
        if (descriptor.getCustomUiPath() != null) {
            zipReader.readFileAsString(folder, descriptor.getCustomUiPath())
                    .ifPresent(descriptor::setCustomUi);
        }
        log.info("Registering Widget {}", descriptor);
        engineService.registerWidget(descriptor);
    }

}
