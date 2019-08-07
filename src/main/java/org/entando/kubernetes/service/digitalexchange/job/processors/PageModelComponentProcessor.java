package org.entando.kubernetes.service.digitalexchange.job.processors;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.service.digitalexchange.entandocore.EntandoEngineService;
import org.entando.kubernetes.service.digitalexchange.job.ZipReader;
import org.entando.kubernetes.service.digitalexchange.job.model.PageModelDescriptor;
import org.entando.kubernetes.service.digitalexchange.model.DigitalExchange;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
public class PageModelComponentProcessor implements ComponentProcessor<PageModelDescriptor> {

    private final @NonNull EntandoEngineService engineService;

    @Override
    public void processComponent(final DigitalExchange digitalExchange, final String componentId,
                                 final PageModelDescriptor descriptor, final ZipReader zipReader,
                                 final String folder) throws IOException {
        if (descriptor.getTemplatePath() != null) {
            zipReader.readFileAsString(folder, descriptor.getTemplatePath())
                    .ifPresent(descriptor::setTemplate);
        }
        log.info("Registering Page Model {}", descriptor);
        engineService.registerPageModel(descriptor);
    }
}
