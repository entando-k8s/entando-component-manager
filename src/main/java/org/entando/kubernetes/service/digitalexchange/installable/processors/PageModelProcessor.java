package org.entando.kubernetes.service.digitalexchange.installable.processors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.model.digitalexchange.ComponentType;
import org.entando.kubernetes.model.digitalexchange.DigitalExchangeJob;
import org.entando.kubernetes.model.digitalexchange.DigitalExchangeJobComponent;
import org.entando.kubernetes.model.digitalexchange.InstallableInstallResult;
import org.entando.kubernetes.service.digitalexchange.entandocore.EntandoEngineService;
import org.entando.kubernetes.service.digitalexchange.installable.ComponentProcessor;
import org.entando.kubernetes.service.digitalexchange.installable.Installable;
import org.entando.kubernetes.service.digitalexchange.job.ZipReader;
import org.entando.kubernetes.service.digitalexchange.job.model.ComponentDescriptor;
import org.entando.kubernetes.service.digitalexchange.job.model.ComponentSpecDescriptor;
import org.entando.kubernetes.service.digitalexchange.job.model.PageModelDescriptor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static java.util.Optional.ofNullable;

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

    private final EntandoEngineService engineService;

    @Override
    public List<? extends Installable> process(final DigitalExchangeJob job, final ZipReader zipReader,
                                               final ComponentDescriptor descriptor) throws IOException {

        final Optional<List<String>> pageModelsDescriptor = ofNullable(descriptor.getComponents()).map(ComponentSpecDescriptor::getPageModels);
        final List<Installable> installables = new LinkedList<>();

        if (pageModelsDescriptor.isPresent()) {
            for (final String fileName : pageModelsDescriptor.get()) {
                final PageModelDescriptor pageModelDescriptor = zipReader.readDescriptorFile(fileName, PageModelDescriptor.class);
                if (pageModelDescriptor.getTemplatePath() != null) {
                    pageModelDescriptor.setTemplate(zipReader.readFileAsString(getFolder(fileName), pageModelDescriptor.getTemplatePath()));
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
        public CompletableFuture<InstallableInstallResult> install() {
            return CompletableFuture.supplyAsync(() -> {
                log.info("Registering Page Model {}", representation.getCode());
                return wrap(() ->engineService.registerPageModel(representation));
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
