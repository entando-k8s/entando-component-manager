package org.entando.kubernetes.model.bundle.processor;

import static java.util.Optional.ofNullable;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.model.bundle.BundleReader;
import org.entando.kubernetes.model.bundle.descriptor.ComponentDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.ComponentSpecDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.PageDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.PageModelDescriptor;
import org.entando.kubernetes.model.bundle.installable.Installable;
import org.entando.kubernetes.model.bundle.installable.PageInstallable;
import org.entando.kubernetes.model.bundle.installable.PageModelInstallable;
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
public class PageProcessor implements ComponentProcessor {

    private final EntandoCoreService engineService;

    @Override
    public List<Installable> process(final DigitalExchangeJob job, final BundleReader npr,
                                               final ComponentDescriptor descriptor) throws IOException {

        List<String> pageModelsDescriptor = ofNullable(descriptor.getComponents())
                .map(ComponentSpecDescriptor::getPageModels)
                .orElse(Collections.emptyList());
        List<String> pageDescriptorList = ofNullable(descriptor.getComponents())
                .map(ComponentSpecDescriptor::getPages)
                .orElse(Collections.emptyList());
        List<Installable> installables = new LinkedList<>();

        for (final String fileName : pageModelsDescriptor) {
            final PageModelDescriptor pageModelDescriptor = npr.readDescriptorFile(fileName, PageModelDescriptor.class);
            if (pageModelDescriptor.getTemplatePath() != null) {
                String tp = getRelativePath(fileName, pageModelDescriptor.getTemplatePath());
                pageModelDescriptor.setTemplate(npr.readFileAsString(tp));
            }
            installables.add(new PageModelInstallable(engineService, pageModelDescriptor));
        }

        for (String fileName : pageDescriptorList)  {
            PageDescriptor pageDescriptor = npr.readDescriptorFile(fileName, PageDescriptor.class);
            installables.add(new PageInstallable(engineService, pageDescriptor));
        }

        return installables;
    }

    @Override
    public boolean shouldProcess(final ComponentType componentType) {
        return componentType == ComponentType.PAGE_MODEL || componentType == ComponentType.PAGE;
    }

    @Override
    public void uninstall(final DigitalExchangeJobComponent component) {
        if (component.getComponentType().equals(ComponentType.PAGE_MODEL)) {
            log.info("Removing PageModel {}", component.getName());
            engineService.deletePageModel(component.getName());
        }
        if (component.getComponentType().equals(ComponentType.PAGE)) {
            log.info("Removing Page {}", component.getName());
            engineService.deletePage(component.getName());
        }
    }

}
