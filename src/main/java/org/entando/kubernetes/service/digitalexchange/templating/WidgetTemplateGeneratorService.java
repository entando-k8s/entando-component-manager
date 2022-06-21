package org.entando.kubernetes.service.digitalexchange.templating;

import org.entando.kubernetes.model.bundle.descriptor.widget.WidgetDescriptor;
import org.entando.kubernetes.model.bundle.reader.BundleReader;

public interface WidgetTemplateGeneratorService {

    String generateWidgetTemplate(String descriptorFileName, WidgetDescriptor widgetDescriptor, BundleReader bundleReader);
}
