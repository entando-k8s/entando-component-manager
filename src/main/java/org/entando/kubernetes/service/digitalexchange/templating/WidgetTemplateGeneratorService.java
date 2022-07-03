package org.entando.kubernetes.service.digitalexchange.templating;

import java.util.List;
import java.util.Map;
import org.entando.kubernetes.model.bundle.descriptor.widget.WidgetDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.widget.WidgetDescriptor.ApiClaim;
import org.entando.kubernetes.model.bundle.reader.BundleReader;

public interface WidgetTemplateGeneratorService {

    String generateWidgetTemplate(String descriptorFileName, WidgetDescriptor widgetDescriptor, BundleReader bundleReader);

    String updateWidgetTemplate(String ftl, List<ApiClaim> apiClaims, Map<String, String> pluginIngressPathMap,
            String currentBundleId);

    boolean checkApiClaim(ApiClaim apiClaim, String bundleId);
}
