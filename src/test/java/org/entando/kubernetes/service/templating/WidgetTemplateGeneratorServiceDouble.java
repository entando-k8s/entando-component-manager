package org.entando.kubernetes.service.templating;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.entando.kubernetes.model.bundle.descriptor.widget.WidgetDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.widget.WidgetDescriptor.ApiClaim;
import org.entando.kubernetes.model.bundle.reader.BundleReader;
import org.entando.kubernetes.service.digitalexchange.templating.WidgetTemplateGeneratorService;

public class WidgetTemplateGeneratorServiceDouble implements WidgetTemplateGeneratorService {

    public static final String FTL_TEMPLATE = "my custom ftl template";
    public static final String API_KEY = "int-api";
    public static final String API_URL = "/ingress-path/service-id-1/path";

    @Override
    public String generateWidgetTemplate(String descriptorFileName, WidgetDescriptor widgetDescriptor,
            BundleReader bundleReader) {
        return FTL_TEMPLATE;
    }

    @Override
    public String updateWidgetTemplate(String ftl, List<ApiClaim> apiClaims,
            String currentBundleId) {
        return ftl;
    }

    @Override
    public boolean checkApiClaim(ApiClaim apiClaim, String bundleId) {
        return true;
    }

    @Override
    public SystemParams generateSystemParamsWithIngressPath(List<ApiClaim> apiClaimList, String bundleId) {
        Map<String, ApiUrl> api = new HashMap<>();
        api.put(API_KEY, new ApiUrl(API_URL));
        SystemParams systemParams = new SystemParams(api);
        return systemParams;
    }

}
