package org.entando.kubernetes.model.bundle.processor;

import static java.util.Optional.ofNullable;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.model.bundle.NpmBundleReader;
import org.entando.kubernetes.model.bundle.descriptor.ComponentDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.ComponentSpecDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.PageModelDescriptor;
import org.entando.kubernetes.model.bundle.installable.Installable;
import org.entando.kubernetes.model.digitalexchange.ComponentType;
import org.entando.kubernetes.model.digitalexchange.DigitalExchangeJob;
import org.entando.kubernetes.model.digitalexchange.DigitalExchangeJobComponent;
import org.entando.kubernetes.service.digitalexchange.entandocore.EntandoCoreService;
import org.springframework.stereotype.Service;

/**
 * Processor to create Page Models, can handle descriptors
 * with template embedded or a separate template file.
 *
 * @author Sergio Marcelino
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PageModelProcessor implements ComponentProcessor {

    private final EntandoCoreService engineService;

    @Override
    public List<Installable> process(final DigitalExchangeJob job, final NpmBundleReader npr,
                                               final ComponentDescriptor descriptor) throws IOException {

        final Optional<List<String>> pageModelsDescriptor = ofNullable(descriptor.getComponents()).map(ComponentSpecDescriptor::getPageModels);
        final List<Installable> installables = new LinkedList<>();

        if (pageModelsDescriptor.isPresent()) {
            for (final String fileName : pageModelsDescriptor.get()) {
                final PageModelDescriptor pageModelDescriptor = npr.readDescriptorFile(fileName, PageModelDescriptor.class);
                if (pageModelDescriptor.getTemplatePath() != null) {
                    String tp = getRelativePath(fileName, pageModelDescriptor.getTemplatePath());
                    pageModelDescriptor.setTemplate(npr.readFileAsString(tp));
                }
                installables.add(new PageModelInstallable(pageModelDescriptor));
            }
        }

        return installables;
    }

    @Override
    public boolean shouldProcess(final ComponentType componentType) {
        return componentType == ComponentType.PAGE_MODEL;
    }

    @Override
    public void uninstall(final DigitalExchangeJobComponent component) {
        log.info("Removing PageModel {}", component.getName());
        engineService.deletePageModel(component.getName());
    }

    public class PageModelInstallable extends Installable<PageModelDescriptor> {

        private PageModelInstallable(final PageModelDescriptor pageModelDescriptor) {
            super(pageModelDescriptor);
        }

        @Override
        public CompletableFuture install() {
            return CompletableFuture.runAsync(() -> {
                log.info("Registering Page Model {}", representation.getCode());
                engineService.registerPageModel(representation);
            });
        }

        @Override
        public ComponentType getComponentType() {
            return ComponentType.PAGE_MODEL;
        }

        @Override
        public String getName() {
            return representation.getCode();
        }

    }
}
