package org.entando.kubernetes.model.bundle.processor;

import static java.util.Optional.ofNullable;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.model.bundle.BundleReader;
import org.entando.kubernetes.model.bundle.descriptor.ComponentDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.ComponentSpecDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.ContentModelDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.ContentTypeDescriptor;
import org.entando.kubernetes.model.bundle.installable.ContentModelInstallable;
import org.entando.kubernetes.model.bundle.installable.ContentTypeInstallable;
import org.entando.kubernetes.model.bundle.installable.Installable;
import org.entando.kubernetes.model.digitalexchange.ComponentType;
import org.entando.kubernetes.model.digitalexchange.DigitalExchangeJob;
import org.entando.kubernetes.model.digitalexchange.DigitalExchangeJobComponent;
import org.entando.kubernetes.service.digitalexchange.entandocore.EntandoCoreService;
import org.springframework.stereotype.Service;

/**
 * Processor to handle CMS Plugin stuff to be stored by Entando.
 * Currently creating ContentTypes and ContentModels
 *
 * @author Sergio Marcelino
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CmsProcessor implements ComponentProcessor {

    private final EntandoCoreService engineService;

    @Override
    public List<Installable> process(final DigitalExchangeJob job, final BundleReader npr,
                                               final ComponentDescriptor descriptor) throws IOException {

        final Optional<List<String>> contentTypesDescriptor = ofNullable(descriptor.getComponents())
                .map(ComponentSpecDescriptor::getContentTypes);
        final Optional<List<String>> contentModelsDescriptor = ofNullable(descriptor.getComponents())
                .map(ComponentSpecDescriptor::getContentModels);
        final List<Installable> installables = new LinkedList<>();

        if (contentTypesDescriptor.isPresent()) {
            for (final String fileName : contentTypesDescriptor.get()) {
                final ContentTypeDescriptor contentTypeDescriptor = npr.readDescriptorFile(fileName, ContentTypeDescriptor.class);
                installables.add(new ContentTypeInstallable(engineService, contentTypeDescriptor));
            }
        }

        if (contentModelsDescriptor.isPresent()) {
            for (final String fileName : contentModelsDescriptor.get()) {
                final ContentModelDescriptor contentModelDescriptor = npr.readDescriptorFile(fileName, ContentModelDescriptor.class);
                if (contentModelDescriptor.getContentShapePath() != null) {
                  String csPath = getRelativePath(fileName, contentModelDescriptor.getContentShapePath());
                  contentModelDescriptor.setContentShape(npr.readFileAsString(csPath));
                }
                installables.add(new ContentModelInstallable(engineService, contentModelDescriptor));
            }
        }

        return installables;
    }

    @Override
    public boolean shouldProcess(final ComponentType componentType) {
        return componentType == ComponentType.CONTENT_MODEL || componentType == ComponentType.CONTENT_TYPE;
    }

    @Override
    public void uninstall(final DigitalExchangeJobComponent component) {
        if (component.getComponentType() == ComponentType.CONTENT_MODEL) {
            log.info("Removing Content Model {}", component.getName());
            engineService.deleteContentModel(component.getName());
        } else if (component.getComponentType() == ComponentType.CONTENT_TYPE) {
            log.info("Removing Content Type {}", component.getName());
            engineService.deleteContentType(component.getName());
        }
    }

}
