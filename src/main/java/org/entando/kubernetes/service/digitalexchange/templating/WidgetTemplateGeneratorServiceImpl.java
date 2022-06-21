package org.entando.kubernetes.service.digitalexchange.templating;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FilenameUtils;
import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.model.bundle.BundleProperty;
import org.entando.kubernetes.model.bundle.descriptor.widget.WidgetDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.widget.WidgetDescriptor.ApiClaim;
import org.entando.kubernetes.model.bundle.reader.BundleReader;
import org.entando.kubernetes.model.job.PluginDataEntity;
import org.entando.kubernetes.repository.PluginDataRepository;
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

    private final PluginDataRepository apiPathRepository;

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

    protected String createResourceTags(String descriptorFileName, BundleReader bundleReader) {

        final String widgetFolder = FilenameUtils.removeExtension(descriptorFileName);
        final String bundleId = BundleUtilities.removeProtocolAndGetBundleId(bundleReader.getBundleUrl());

        String ftl = bundleReader.getWidgetResourcesOfType(widgetFolder, JS_TYPE).stream()
                .map(file -> formatTagFilePath(bundleReader, SCRIPT_TAG, file, bundleId))
                .collect(Collectors.joining("\n"));

        ftl += "\n\n";

        ftl += bundleReader.getWidgetResourcesOfType(widgetFolder, CSS_TYPE).stream()
                .map(file -> formatTagFilePath(bundleReader, CSS_TAG, file, bundleId))
                .collect(Collectors.joining("\n"));

        return ftl;
    }

    private String formatTagFilePath(BundleReader bundleReader, String tag, String file, String bundleId) {
        try {
            final String fullFile = BundleUtilities.buildFullBundleResourcePath(bundleReader,
                    BundleProperty.WIDGET_FOLDER_PATH, file, bundleId);
            return String.format(tag, fullFile);
        } catch (Exception e) {
            throw new EntandoComponentManagerException(e);
        }
    }

    protected String createAssignTag(WidgetDescriptor descriptor) throws JsonProcessingException {
        final MfeSystemConfig mfeSystemConfig = toSystemParams(descriptor.getApiClaims(),
                descriptor.getDescriptorMetadata().getPluginIngressPathMap());
        return String.format(ASSIGN_TAG, jsonMapper.writeValueAsString(mfeSystemConfig).replace("\"", "'"));
    }

    protected String createCustomElementTag(WidgetDescriptor descriptor) {
        return String.format(CUSTOM_ELEMENT_TAG, descriptor.getCustomElement());
    }

    protected String getApiUrl(WidgetDescriptor.ApiClaim apiClaim, Map<String, String> pluginIngressPathMap) {

        String ingressPath;
        if (apiClaim.getType().equals(WidgetDescriptor.ApiClaim.INTERNAL_API)) {
            ingressPath = pluginIngressPathMap.get(apiClaim.getPluginName());
        } else {
            ingressPath = apiPathRepository.findByBundleIdAndPluginName(apiClaim.getBundleId(), apiClaim.getPluginName())
                    .map(PluginDataEntity::getEndpoint)
                    .orElse("");
        }

        if (ObjectUtils.isEmpty(ingressPath)) {
            throw new EntandoComponentManagerException("Can't supply the claimed API " + apiClaim.getName());
        }

        return ingressPath;
    }

    protected MfeSystemConfig toSystemParams(List<ApiClaim> apiClaimList, Map<String, String> pluginIngressPathMap) {
        final Map<String, ApiUrl> apiMap = Optional.ofNullable(apiClaimList).orElseGet(ArrayList::new).stream()
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
