package org.entando.kubernetes.service.digitalexchange.templating;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.AbstractMap.SimpleEntry;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FilenameUtils;
import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.model.bundle.descriptor.widget.WidgetDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.widget.WidgetDescriptor.ApiClaim;
import org.entando.kubernetes.model.bundle.reader.BundleReader;
import org.entando.kubernetes.model.job.PluginAPIDataEntity;
import org.entando.kubernetes.repository.PluginAPIDataRepository;
import org.entando.kubernetes.service.digitalexchange.BundleUtilities;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

@Service
@RequiredArgsConstructor
public class WidgetTemplateGeneratorServiceImpl implements WidgetTemplateGeneratorService {

    public static final String JS_TYPE = "js";
    public static final String CSS_TYPE = "css";
    public static final String APS_CORE_TAG = "<#assign wp=JspTaglibs[\"/aps-core\"]>\n\n";
    public static final String APP_BASE_URL_TAG = "<@wp.info key=\"systemParam\" paramName=\"applicationBaseURL\""
            + " var=\"systemParam_applicationBaseURL\" />\n\n";
    public static final String SCRIPT_TAG = "<script src=\"<@wp.resourceURL />%s\"></script>";
    public static final String CSS_TAG = "<link href=\"<@wp.resourceURL />%s\" rel=\"stylesheet\">";
    public static final String ASSIGN_TAG = "<#assign mfeSystemConfig>%s</#assign>";
    public static final String CUSTOM_ELEMENT_TAG = "<%s config=\"${mfeSystemConfig}\"/>";
    public static final String APPLICATION_BASEURL_PARAM = "${systemParam_applicationBaseURL}";

    private final PluginAPIDataRepository apiPathRepository;

    private ObjectMapper jsonMapper = new ObjectMapper();

    @Override
    public String generateWidgetTemplate(String descriptorFileName, WidgetDescriptor descriptor,
            BundleReader bundleReader) {
        try {
            return APS_CORE_TAG
                    + APP_BASE_URL_TAG
                    + createResourceTags(descriptorFileName, bundleReader) + "\n"
                    + "\n" + createAssignTag(descriptor) + "\n"
                    + "\n" + createCustomElementTag(descriptor);
        } catch (Exception e) {
            throw new EntandoComponentManagerException(
                    "An error occurred during the generation of the FTL for the widget " + descriptor.getCode(), e);
        }
    }

    protected String createResourceTags(String descriptorFileName, BundleReader bundleReader) throws IOException {

        final String widgetFolder = FilenameUtils.removeExtension(descriptorFileName);

        final String bundleNameFolder = BundleUtilities.determineBundleResourceRootFolder(bundleReader);
        final String signedBundleFolder = BundleUtilities.appendHashToBundleFolder(bundleReader, bundleNameFolder);

        String ftl = bundleReader.getWidgetResourcesOfType(widgetFolder, JS_TYPE).stream()
                .map(file -> formatTagFilePath(SCRIPT_TAG, signedBundleFolder, file))
                .collect(Collectors.joining("\n"));

        ftl += "\n\n";

        ftl += bundleReader.getWidgetResourcesOfType(widgetFolder, CSS_TYPE).stream()
                .map(file -> formatTagFilePath(CSS_TAG, signedBundleFolder, file))
                .collect(Collectors.joining("\n"));

        return ftl;
    }

    private String formatTagFilePath(String tag, String signedBundleFolder, String file) {
        return String.format(tag, Paths.get(signedBundleFolder, file));
    }

    protected String createAssignTag(WidgetDescriptor descriptor) throws JsonProcessingException {
        final MfeSystemConfig mfeSystemConfig = toSystemParams(descriptor.getApiClaims(),
                descriptor.getDescriptorMetadata().getPluginIngressPathMap());
        return String.format(ASSIGN_TAG, jsonMapper.writeValueAsString(mfeSystemConfig));
    }

    protected String createCustomElementTag(WidgetDescriptor descriptor) {
        return String.format(CUSTOM_ELEMENT_TAG, descriptor.getCustomElement());
    }

    protected String getApiUrl(WidgetDescriptor.ApiClaim apiClaim, Map<String, String> pluginIngressPathMap) {

        String ingressPath;
        if (apiClaim.getType().equals(WidgetDescriptor.ApiClaim.INTERNAL_API)) {
            ingressPath = pluginIngressPathMap.get(apiClaim.getServiceId());
        } else {
            ingressPath = apiPathRepository.findByBundleIdAndServiceId(apiClaim.getBundleId(), apiClaim.getServiceId())
                    .map(PluginAPIDataEntity::getIngressPath)
                    .orElse(null);
        }

        if (ObjectUtils.isEmpty(ingressPath)) {
            throw new EntandoComponentManagerException("Can't supply the claimed API " + apiClaim.getName());
        }

        return ingressPath;
    }

    protected MfeSystemConfig toSystemParams(List<ApiClaim> apiClaimList, Map<String, String> pluginIngressPathMap) {
        final Map<String, ApiUrl> apiMap = apiClaimList.stream()
                .map(ac -> {
                    String ingressPath = APPLICATION_BASEURL_PARAM + getApiUrl(ac, pluginIngressPathMap);
                    return new SimpleEntry<>(ac.getName(), new ApiUrl(ingressPath));
                })
                .collect(Collectors.toMap(
                        SimpleEntry::getKey,
                        SimpleEntry::getValue));
        return new MfeSystemConfig(new SystemParams(apiMap));
    }

    @AllArgsConstructor
    @Getter
    private class MfeSystemConfig {

        private SystemParams systemParams;
    }

    @AllArgsConstructor
    @Getter
    private class SystemParams {

        private Map<String, ApiUrl> api;
    }

    @AllArgsConstructor
    @Getter
    private class ApiUrl {

        private String url;
    }
}
