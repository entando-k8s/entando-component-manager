package org.entando.kubernetes.service.digitalexchange.templating;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;
import org.entando.kubernetes.model.bundle.descriptor.widget.WidgetDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.widget.WidgetDescriptor.ApiClaim;
import org.entando.kubernetes.model.bundle.reader.BundleReader;

public interface WidgetTemplateGeneratorService {

    String generateWidgetTemplate(String descriptorFileName, WidgetDescriptor widgetDescriptor,
            BundleReader bundleReader);

    String updateWidgetTemplate(String ftl, List<ApiClaim> apiClaims,
            String currentBundleId);

    boolean checkApiClaim(ApiClaim apiClaim, String bundleId);

    /**
     * This method generates a System Params json object that contains the settings needed to run the widget as a
     * configurations API.
     *
     * @param apiClaimList a list of ApiClaim objects containing the external / internal API paths
     * @param bundleId     the 8-digits code used as bundle identifier
     * @return a System Params json object
     */
    SystemParams generateSystemParamsWithIngressPath(List<ApiClaim> apiClaimList, String bundleId);


    @AllArgsConstructor
    @Jacksonized
    @Builder
    @Getter
    class SystemParams {

        private Map<String, ApiUrl> api;
    }

    @AllArgsConstructor
    @Jacksonized
    @Builder
    @Getter
    class ApiUrl {

        private String url;
    }

}
