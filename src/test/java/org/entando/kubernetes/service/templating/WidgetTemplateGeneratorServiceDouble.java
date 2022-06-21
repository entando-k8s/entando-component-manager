package org.entando.kubernetes.service.templating;

import org.entando.kubernetes.model.bundle.descriptor.widget.WidgetDescriptor;
import org.entando.kubernetes.model.bundle.reader.BundleReader;
import org.entando.kubernetes.service.digitalexchange.templating.WidgetTemplateGeneratorService;

public class WidgetTemplateGeneratorServiceDouble implements WidgetTemplateGeneratorService {

    public static final String FTL_TEMPLATE = "my custom ftl template";

    @Override
    public String generateWidgetTemplate(String descriptorFileName, WidgetDescriptor widgetDescriptor, BundleReader bundleReader) {
        return FTL_TEMPLATE;
    }
}
