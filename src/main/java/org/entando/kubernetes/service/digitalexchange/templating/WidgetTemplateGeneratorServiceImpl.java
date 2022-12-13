package org.entando.kubernetes.service.digitalexchange.templating;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
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
import org.entando.kubernetes.model.bundle.descriptor.widget.WidgetDescriptor.Param;
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
    public static final String ASSIGN_TAG_MFE_CONFIG = "<#assign mfeConfig>%s</#assign>";
    public static final String ASSIGN_TAG_GENERIC = "<#assign %s>%s</#assign>";

    public static final String ASSIGN_TAG_FROM_VAR_NULLSAFE = "<#assign %s>${(%s)!\"\"}</#assign>";
    public static final String CUSTOM_ELEMENT_TAG =
            "<%s config=\"<#outputformat 'HTML'>${mfeConfig}</#outputformat>\"/>";
    public static final String APPLICATION_BASEURL_PARAM = "";
    public static final String DEFAULT_CONTEXT_PARAM = "systemParam_applicationBaseURL";
    public static final String PARAM_TYPE_PAGE = "page";
    public static final String PARAM_TYPE_INFO = "info";
    public static final String PARAM_TYPE_SYSTEM_PARAM = "systemParam";
    public static final String CONFIG_KEY_SYSTEM_PARAMS = "systemParams";
    public static final String CONFIG_KEY_MFE_PARAMS = "params";
    public static final String CONFIG_KEY_CONTEXT_PARAMS = "contextParams";
    public static final String CONFIG_KEY_API_CLAIM_PARAMS = "apiClaim";
    public static final String FTL_WIDGETS_PARAM_PREFIX = "widget";
    public static final String DESCRIPTOR_OBJECT_MEMBER_SEPARATOR = "_";
    public static final String FTL_SCOPE_SEPARATOR = "_";
    public static final String PAGE_GLOBAL_OBJECT_UPDATE_BASE_WIDGET_PATH_TPL = "<script>\n"
            + "window.entando = {\n"
            + "  ...(window.entando || {}),\n"
            + "};\n"
            + "window.entando.widgets = {\n"
            + "  ...(window.entando.widgets || {}),\n"
            + "};\n"
            + "window.entando.widgets[\"%s\"]={\n"
            + "  \"basePath\": \"<@wp.resourceURL />%s\"\n"
            + "}\n"
            + "</script>";
    public static final String PLACEHOLDER_FOR_API_URL_EXTRACTION = "<#assign PLACEHOLDER_FOR_API_URL_EXTRACTION></#assign>";
    public static final String FTL_DASH_REPLACEMENT_IN_VARS = "_DASH_";

    private final PluginDataRepository apiPathRepository;

    private ObjectMapper jsonMapper = new ObjectMapper();

    @Override
    public String generateWidgetTemplate(String descriptorFileName, WidgetDescriptor descriptor,
            BundleReader bundleReader) {
        try {
            return APS_HEAD + "\n" + APS_CORE_TAG + "\n" + "\n"
                    + generateCodeForContextParametersExtraction(descriptor) + "\n"
                    + generateCodeForMfeParametersExtraction(descriptor)
                    + "\n" + PLACEHOLDER_FOR_API_URL_EXTRACTION + "\n"
                    + "\n" + generateCodeForPageGlobalObjectUpdate(descriptorFileName, descriptor, bundleReader) + "\n"
                    + "\n" + generateCodeForResourcesInclusion(descriptorFileName, bundleReader) + "\n"
                    + "\n" + generateCodeForMfeConfigObjectCreation(descriptor) + "\n"
                    + "\n" + generateCodeForCustomElementInvocation(descriptor);
        } catch (Exception e) {
            throw new EntandoComponentManagerException(
                    "An error occurred during the generation of the FTL for the " + FTL_WIDGETS_PARAM_PREFIX + " "
                            + descriptor.getCode(), e);
        }
    }

    /**
     * Updates the FTL code. It replaces PLACEHOLDER_FOR_API_URL_EXTRACTION with code to properly assign the apiClaim
     * variables that are then referenced in the mfe configuration object
     */
    @Override
    public String updateWidgetTemplate(String ftl, List<ApiClaim> apiClaims, String currentBundleId) {
        var res = new StringBuilder();
        if (apiClaims != null) {
            for (var apiClaim : apiClaims) {
                var ftlVarName = ftlScopedVar(CONFIG_KEY_API_CLAIM_PARAMS, apiClaim.getName());
                String resolvedApiUrl = mustFindApiUrl(apiClaim, currentBundleId);
                if (resolvedApiUrl.endsWith("/")) {
                    resolvedApiUrl = resolvedApiUrl.substring(0, resolvedApiUrl.length() - 1);
                }
                res.append(String.format(ASSIGN_TAG_GENERIC, ftlVarName, resolvedApiUrl));
                res.append('\n');
            }
            if (res.length() > 0) {
                res.setLength(res.length() - 1);
            }
        }
        return ftl.replace(PLACEHOLDER_FOR_API_URL_EXTRACTION, res.toString());
    }

    public boolean checkApiClaim(ApiClaim apiClaim, String bundleId) {
        return findApiUrl(apiClaim, bundleId) != null;
    }

    private String generateCodeForPageGlobalObjectUpdate(String descriptorFileName, WidgetDescriptor descriptor,
            BundleReader bundleReader) {
        final String bundleId = BundleUtilities.removeProtocolAndGetBundleId(bundleReader.getBundleUrl());

        String baseWidgetPath = "";

        try {
            final String widgetFolder = FilenameUtils.removeExtension(descriptorFileName);
            baseWidgetPath = BundleUtilities.buildFullBundleResourcePath(bundleReader,
                    BundleProperty.WIDGET_FOLDER_PATH, widgetFolder, bundleId);

        } catch (IOException e) {
            log.error("Unable to determine the widget base path", e);
        }

        if (baseWidgetPath.endsWith("/")) {
            baseWidgetPath = baseWidgetPath.substring(0, baseWidgetPath.length() - 1);
        }

        return String.format(PAGE_GLOBAL_OBJECT_UPDATE_BASE_WIDGET_PATH_TPL, descriptor.getName(), baseWidgetPath);
    }

    private String generateCodeForMfeParametersExtraction(WidgetDescriptor descriptor) {
        var res = new StringBuilder();
        var params = descriptor.getParams();
        if (params != null) {
            for (var p : params) {
                String scopedVarName = ftlScopedVar(FTL_WIDGETS_PARAM_PREFIX, p.getName());
                res.append(String.format("<@wp.currentWidget param=\"%s\" configParam=\"%s\" var=\"%s\" />\n",
                        "config", p.getName(), scopedVarName));
                res.append(String.format(ASSIGN_TAG_FROM_VAR_NULLSAFE + "\n", scopedVarName, scopedVarName));
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
                        res.append(String.format("<@wp.currentPage param=\"%s\" var=\"%s\" />\n",
                                paramName, ftlScopedVar(paramType, paramName)));
                        break;
                    case PARAM_TYPE_INFO:
                        res.append(String.format("<@wp.info key=\"%s\" var=\"%s\" />\n",
                                paramName, ftlScopedVar(paramType, paramName)));
                        break;
                    case PARAM_TYPE_SYSTEM_PARAM:
                        res.append(String.format("<@wp.info key=\"%s\" paramName=\"%s\" var=\"%s\" />\n",
                                paramType, paramName, ftlScopedVar(paramType, paramName)));
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

        ftl += "\n";

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

    protected String generateCodeForMfeConfigObjectCreation(WidgetDescriptor descriptor)
            throws JsonProcessingException {
        Map<String, Object> res = new HashMap<>();
        res.put(CONFIG_KEY_SYSTEM_PARAMS, generateSystemParamsForConfig(descriptor.getApiClaims()));
        res.put(CONFIG_KEY_MFE_PARAMS, toMfeParamsForConfig(descriptor.getParams()));
        res.put(CONFIG_KEY_CONTEXT_PARAMS, toContextParamsForConfig(descriptor.getContextParams()));
        return String.format(ASSIGN_TAG_MFE_CONFIG, jsonMapper.writeValueAsString(res));
    }

    protected String generateCodeForCustomElementInvocation(WidgetDescriptor descriptor) {
        return String.format(CUSTOM_ELEMENT_TAG, descriptor.getCustomElement());
    }

    protected String mustFindApiUrl(ApiClaim apiClaim, String currentBundleId) {

        String ingressPath = findApiUrl(apiClaim, currentBundleId);

        if (ObjectUtils.isEmpty(ingressPath)) {
            throw new EntandoComponentManagerException("Can't supply the claimed API " + apiClaim.getName());
        }

        return ingressPath;
    }

    private String findApiUrl(ApiClaim apiClaim, String currentBundleId) {
        String apiBundleId = (apiClaim.getType().equals(ApiClaim.INTERNAL_API))
                ? currentBundleId : apiClaim.getBundleId();

        String ingressPath = apiPathRepository
                .findByBundleIdAndPluginName(apiBundleId, apiClaim.getPluginName())
                .map(PluginDataEntity::getEndpoint)
                .orElse("");

        return ingressPath;
    }

    @Override
    public SystemParams generateSystemParamsWithIngressPath(List<ApiClaim> apiClaimList, String bundleId) {
        var apiMap = Optional.ofNullable(apiClaimList).orElseGet(ArrayList::new).stream()
                .map(ac -> {
                    String key = ac.getName();
                    String url = mustFindApiUrl(ac, bundleId);
                    if (url.endsWith("/")) {
                        url = url.substring(0, url.length() - 1);
                    }
                    return new SimpleEntry<>(key, new ApiUrl(url));
                })
                .collect(Collectors.toMap(SimpleEntry::getKey, SimpleEntry::getValue));

        return new SystemParams(apiMap);

    }

    protected FtlSystemParams generateSystemParamsForConfig(List<ApiClaim> apiClaimList) {
        //~
        var apiMap = Optional.ofNullable(apiClaimList).orElseGet(ArrayList::new).stream()
                .map(ac -> {
                    String key = ac.getName();
                    String val = ftlScopedVar(CONFIG_KEY_API_CLAIM_PARAMS, ac.getName(), true);
                    return new SimpleEntry<>(key, new FtlApiUrl(APPLICATION_BASEURL_PARAM + val));
                })
                .collect(Collectors.toMap(SimpleEntry::getKey, SimpleEntry::getValue));
        return new FtlSystemParams(apiMap);
    }

    private static String ftlScopedVar(String scope, String value) {
        return ftlScopedVar(scope, value, false);
    }

    private static String ftlScopedVar(String scope, String value, boolean interpolated) {
        String varName = scope + FTL_SCOPE_SEPARATOR + value
                .replace(FTL_DASH_REPLACEMENT_IN_VARS, "_" + FTL_DASH_REPLACEMENT_IN_VARS + "_")
                .replace("-", FTL_DASH_REPLACEMENT_IN_VARS);
        return interpolated ? "${" + varName + "}" : varName;
    }

    private HashMap<String, String> toContextParamsForConfig(List<String> contextParams) {
        var res = new HashMap<String, String>();
        if (contextParams != null) {
            for (String p : contextParams) {
                var v = parseContextParamFromDescriptor(p);
                if (!v.isEmpty()) {
                    String varType = v.get(0);
                    String varName = v.get(1);
                    res.put(p, ftlScopedVar(varType, varName, true));
                }
            }
        }
        return res;
    }


    protected HashMap<String, String> toMfeParamsForConfig(List<Param> params) {
        var res = new HashMap<String, String>();
        if (params != null) {
            for (var p : params) {
                res.put(p.getName(), ftlScopedVar(FTL_WIDGETS_PARAM_PREFIX, p.getName(), true));
            }
        }
        return res;
    }

    @AllArgsConstructor
    @Getter
    private static class FtlSystemParams {

        private Map<String, FtlApiUrl> api;
    }

    @AllArgsConstructor
    @Getter
    private static class FtlApiUrl {

        private String url;
    }

}
