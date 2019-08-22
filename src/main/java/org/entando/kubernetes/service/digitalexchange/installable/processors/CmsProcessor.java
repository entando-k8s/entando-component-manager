package org.entando.kubernetes.service.digitalexchange.installable.processors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.model.digitalexchange.ComponentType;
import org.entando.kubernetes.model.digitalexchange.DigitalExchangeJob;
import org.entando.kubernetes.model.digitalexchange.DigitalExchangeJobComponent;
import org.entando.kubernetes.service.digitalexchange.entandocore.EntandoEngineService;
import org.entando.kubernetes.service.digitalexchange.installable.ComponentProcessor;
import org.entando.kubernetes.service.digitalexchange.installable.Installable;
import org.entando.kubernetes.service.digitalexchange.job.ZipReader;
import org.entando.kubernetes.service.digitalexchange.job.model.ComponentDescriptor;
import org.entando.kubernetes.service.digitalexchange.job.model.ComponentSpecDescriptor;
import org.entando.kubernetes.service.digitalexchange.job.model.ContentModelDescriptor;
import org.entando.kubernetes.service.digitalexchange.job.model.ContentTypeDescriptor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static java.util.Optional.ofNullable;

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

    private final EntandoEngineService engineService;

    @Override
    public List<? extends Installable> process(final DigitalExchangeJob job, final ZipReader zipReader,
                                               final ComponentDescriptor descriptor) throws IOException {

        final Optional<List<String>> contentTypesDescriptor = ofNullable(descriptor.getComponents())
                .map(ComponentSpecDescriptor::getContentTypes);
        final Optional<List<String>> contentModelsDescriptor = ofNullable(descriptor.getComponents())
                .map(ComponentSpecDescriptor::getContentModels);
        final List<Installable> installables = new LinkedList<>();

        if (contentTypesDescriptor.isPresent()) {
            for (final String fileName : contentTypesDescriptor.get()) {
                final ContentTypeDescriptor contentTypeDescriptor = zipReader.readDescriptorFile(fileName, ContentTypeDescriptor.class);
                installables.add(new ContentTypeInstallable(contentTypeDescriptor));
            }
        }

        if (contentModelsDescriptor.isPresent()) {
            for (final String fileName : contentModelsDescriptor.get()) {
                final ContentModelDescriptor contentModelDescriptor = zipReader.readDescriptorFile(fileName, ContentModelDescriptor.class);
                if (contentModelDescriptor.getContentShapePath() != null) {
                    contentModelDescriptor.setContentShape(zipReader.readFileAsString(getFolder(fileName), contentModelDescriptor.getContentShapePath()));
                }
                installables.add(new ContentModelInstallable(contentModelDescriptor));
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

    public class ContentTypeInstallable extends Installable<ContentTypeDescriptor> {

        private ContentTypeInstallable(final ContentTypeDescriptor contentTypeDescriptor) {
            super(contentTypeDescriptor);
        }

        @Override
        public CompletableFuture install() {
            return CompletableFuture.runAsync(() -> {
                log.info("Registering Content Type {}", representation.getCode());
                engineService.registerContentType(representation);
            });
        }

        @Override
        public ComponentType getComponentType() {
            return ComponentType.CONTENT_TYPE;
        }

        @Override
        public String getName() {
            return representation.getCode();
        }

    }

    public class ContentModelInstallable extends Installable<ContentModelDescriptor> {

        private ContentModelInstallable(final ContentModelDescriptor contentModelDescriptor) {
            super(contentModelDescriptor);
        }

        @Override
        public CompletableFuture install() {
            return CompletableFuture.runAsync(() -> {
                log.info("Registering Content Model {}", representation.getId());
                engineService.registerContentModel(representation);
            });
        }

        @Override
        public ComponentType getComponentType() {
            return ComponentType.CONTENT_MODEL;
        }

        @Override
        public String getName() {
            return representation.getId();
        }

    }
}
