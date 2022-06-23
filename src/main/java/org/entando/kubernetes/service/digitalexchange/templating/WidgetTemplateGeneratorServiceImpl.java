package org.entando.kubernetes.service.digitalexchange.templating;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.model.bundle.BundleProperty;
import org.entando.kubernetes.model.bundle.descriptor.widget.WidgetDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.widget.WidgetDescriptor.ApiClaim;
import org.entando.kubernetes.model.bundle.descriptor.widget.WidgetDescriptor.MfeParam;
import org.entando.kubernetes.model.bundle.reader.BundleReader;
import org.entando.kubernetes.model.job.PluginDataEntity;
import org.entando.kubernetes.repository.PluginDataRepository;
import org.entando.kubernetes.service.digitalexchange.BundleUtilities;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class WidgetTemplateGeneratorServiceImpl implements WidgetTemplateGeneratorService {

    public static final String JS_TYPE = "js";
    public static final String CSS_TYPE = "css";
    public static final String APS_HEAD = "<#ftl output_format=\"undefined\">";
    public static final String APS_CORE_TAG = "<#assign wp=JspTaglibs[\"/aps-core\"]>";
    public static final String SCRIPT_TAG = "<script src=\"<@wp.resourceURL />%s\"></script>";
    public static final String CSS_TAG = "<link href=\"<@wp.resourceURL />%s\" rel=\"stylesheet\">";
    public static final String ASSIGN_TAG = "<#assign mfeConfig>%s</#assign>";
    public static final String CUSTOM_ELEMENT_TAG =
            "<%s config=\"<#outputformat 'HTML'>${mfeConfig}</#outputformat>\"/>";
    public static final String APPLICATION_BASEURL_PARAM = "${systemParam_applicationBaseURL}";
    public static final String DEFAULT_CONTEXT_PARAM = "systemParam_applicationBaseURL";
    public static final String PARAM_TYPE_PAGE = "page";
    public static final String PARAM_TYPE_INFO = "info";
    public static final String PARAM_TYPE_SYSTEM_PARAM = "systemParam";
    public static final String CONFIG_KEY_SYSTEM_PARAMS = "systemParams";
    public static final String CONFIG_KEY_MFE_PARAMS = "params";
    public static final String CONFIG_KEY_CONTEXT_PARAMS = "contextParams";
    public static final String FTL_WIDGETS_PARAM_PREFIX = "widget";
    public static final String DESCRIPTOR_OBJECT_MEMBER_SEPARATOR = "_";
    public static final String FTL_OBJECT_MEMBER_SEPARATOR = "_";

    private final PluginDataRepository apiPathRepository;

    private ObjectMapper jsonMapper = new ObjectMapper();

    @Override
    public String generateWidgetTemplate(String descriptorFileName, WidgetDescriptor descriptor,
            BundleReader bundleReader) {
        try {
            return APS_HEAD + "\n" + APS_CORE_TAG + "\n" + "\n"
                    + generateCodeForContextParametersExtraction(descriptor) + "\n"
                    + generateCodeForMfeParametersExtraction(descriptor) + "\n"
                    + generateCodeForResourcesInclusion(descriptorFileName, bundleReader) + "\n"
                    + "\n" + generateCodeForMfeConfigObjectCreation(descriptor) + "\n"
                    + "\n" + generateCodeForCustomElementInvocation(descriptor);
        } catch (Exception e) {
            throw new EntandoComponentManagerException(
                    "An error occurred during the generation of the FTL for the " + FTL_WIDGETS_PARAM_PREFIX + " " + descriptor.getCode(), e);
        }
    }

    private String generateCodeForMfeParametersExtraction(WidgetDescriptor descriptor) {
        var res = new StringBuilder();
        var params = descriptor.getParams();
        if (params != null) {
            for (var p : params) {
                res.append(String.format("<@wp.currentWidget param=\"%s\" configParam=\"%s\" var=\"%s%s%s\" />\n",
                        "config", p.getName(), FTL_WIDGETS_PARAM_PREFIX, FTL_OBJECT_MEMBER_SEPARATOR, p.getName()));
            }
        }
        return res.toString();
    }

    private String generateCodeForContextParametersExtraction(WidgetDescriptor descriptor) {
        var res = new StringBuilder();
        var contextParams = new ArrayList<String>();
        var tmp = descriptor.getContextParams();
        if (tmp != null) {
            contextParams.addAll(tmp);
        }
        if (!contextParams.contains(DEFAULT_CONTEXT_PARAM)) {
            contextParams.add(DEFAULT_CONTEXT_PARAM);
        }
        for (String p : contextParams) {
            var v = parseContextParamFromDescriptor(p);
            if (v.size() == 2) {
                String paramType = v.get(0);
                String paramName = v.get(1);
                switch (paramType) {
                    case PARAM_TYPE_PAGE:
                        res.append(String.format("<@wp.currentPage param=\"%s\" var=\"%s%s%s\" />\n",
                                paramName, paramType, FTL_OBJECT_MEMBER_SEPARATOR,paramName));
                        break;
                    case PARAM_TYPE_INFO:
                        res.append(String.format("<@wp.info key=\"%s\" var=\"%s%s%s\" />\n",
                                paramName, paramType, FTL_OBJECT_MEMBER_SEPARATOR,paramName));
                        break;
                    case PARAM_TYPE_SYSTEM_PARAM:
                        res.append(String.format("<@wp.info key=\"%s\" paramName=\"%s\" var=\"%s%s%s\" />\n",
                                paramType, paramName, paramType, FTL_OBJECT_MEMBER_SEPARATOR, paramName));
                        break;
                    default:
                        log.error("Received illegal paramType \"{}\" in contextParam \"{}\"", paramType, p);
                }
            } else {
                log.error("Error parsing contextParam \"{}\"", p);
            }
        }
        return res.toString();
    }

    private List<String> parseContextParamFromDescriptor(String contextParam) {
        try {
            int pos = contextParam.indexOf(DESCRIPTOR_OBJECT_MEMBER_SEPARATOR);
            if (pos != -1 && !contextParam.contains("\"")) {
                String t = contextParam.substring(0, pos);
                String v = contextParam.substring(pos + 1);
                return List.of(t, v);
            } else {
                log.error("Invalid contextParam detected {}", contextParam);
                return new ArrayList<>();
            }
        } catch (Exception ex) {
            log.error("Error {} parsing the contextParam {}", ex.getMessage(), contextParam);
            return new ArrayList<>();
        }
    }

    protected String generateCodeForResourcesInclusion(String descriptorFileName, BundleReader bundleReader) {

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

    protected String generateCodeForMfeConfigObjectCreation(WidgetDescriptor descriptor) throws JsonProcessingException {
        Map<String, Object> res = new HashMap<>();
        res.put(CONFIG_KEY_SYSTEM_PARAMS, toSystemParamsForConfig(descriptor.getApiClaims(),
                descriptor.getDescriptorMetadata().getPluginIngressPathMap()));
        res.put(CONFIG_KEY_MFE_PARAMS, toMfeParamsForConfig(descriptor.getParams()));
        res.put(CONFIG_KEY_CONTEXT_PARAMS, toContextParamsForConfig(descriptor.getContextParams()));
        return String.format(ASSIGN_TAG, jsonMapper.writeValueAsString(res));
    }

    protected String generateCodeForCustomElementInvocation(WidgetDescriptor descriptor) {
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

    protected SystemParams toSystemParamsForConfig(List<ApiClaim> apiClaimList, Map<String, String> pluginIngressPathMap) {
        final Map<String, ApiUrl> apiMap = Optional.ofNullable(apiClaimList).orElseGet(ArrayList::new).stream()
                .map(ac -> {
                    String ingressPath = APPLICATION_BASEURL_PARAM + getApiUrl(ac, pluginIngressPathMap);
                    return new SimpleEntry<>(ac.getName(), new ApiUrl(ingressPath));
                })
                .collect(Collectors.toMap(
                        SimpleEntry::getKey,
                        SimpleEntry::getValue));
        return new SystemParams(apiMap);
    }

    private HashMap<String, String> toContextParamsForConfig(List<String> contextParams) {
        var res = new HashMap<String, String>();
        if (contextParams != null) {
            for (String p : contextParams) {
                var v = parseContextParamFromDescriptor(p);
                if (!v.isEmpty()) {
                    String varType = v.get(0);
                    String varName = v.get(1);
                    res.put(p, "${" + varType + FTL_OBJECT_MEMBER_SEPARATOR + varName + "}");
                }
            }
        }
        return res;
    }


    protected HashMap<String, String> toMfeParamsForConfig(List<MfeParam> mfeParams) {
        var res = new HashMap<String, String>();
        if (mfeParams != null) {
            for (var p : mfeParams) {
                res.put(p.getName(), "${" + FTL_WIDGETS_PARAM_PREFIX + FTL_OBJECT_MEMBER_SEPARATOR + p.getName() + "}");
            }
        }
        return res;
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
