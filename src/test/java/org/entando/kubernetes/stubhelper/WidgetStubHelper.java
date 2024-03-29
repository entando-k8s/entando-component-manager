package org.entando.kubernetes.stubhelper;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.entando.kubernetes.model.bundle.ComponentType;
import org.entando.kubernetes.model.bundle.descriptor.DescriptorVersion;
import org.entando.kubernetes.model.bundle.descriptor.widget.WidgetConfigurationDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.widget.WidgetDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.widget.WidgetDescriptor.ApiClaim;
import org.entando.kubernetes.model.bundle.descriptor.widget.WidgetDescriptor.ConfigUi;
import org.entando.kubernetes.model.bundle.descriptor.widget.WidgetDescriptor.DescriptorMetadata;
import org.entando.kubernetes.model.bundle.descriptor.widget.WidgetDescriptor.Param;
import org.entando.kubernetes.model.bundle.descriptor.widget.WidgetDescriptor.WidgetDescriptorBuilder;
import org.entando.kubernetes.service.templating.WidgetTemplateGeneratorServiceDouble;

public class WidgetStubHelper {

    public static final int WIDGET_1_POS = 0;
    public static final String WIDGET_1_NAME = "my-name";
    public static final String WIDGET_1_CODE = "my-code";
    public static final String LANG_1 = "it";
    public static final String LANG_2 = "en";
    public static final String TITLE_1 = "titolo";
    public static final String TITLE_2 = "title";
    public static final Map<String, String> TITLES_MAP = Map.of(LANG_1, TITLE_1, LANG_2, TITLE_2);
    public static final String GROUP = "free";
    public static final String CUSTOM_UI = "<h3>hello</h3>";
    public static final String CUSTOM_UI_PATH = "src/test/resources/bundle/widgets/widget.ftl";
    public static final String CUSTOM_ELEMENT = "myCustomElement";
    public static final String BUNDLE_CODE = "my-bundle";
    public static final String CONFIG_WIDGET = "conf_widget";
    public static final String API_CLAIM_1_NAME = "int-api";
    public static final String API_CLAIM_1_TYPE = "internal";
    public static final String API_CLAIM_1_SERVICE_ID = "service-id-1";
    public static final String API_CLAIM_1_BUNDLE_ID = "a1a1a1a1";
    public static final String API_CLAIM_2_NAME = "ext-api";
    public static final String API_CLAIM_2_TYPE = "external";
    public static final String API_CLAIM_2_SERVICE_ID = "service-id-2";
    public static final String API_CLAIM_2_BUNDLE_ID = "a2a2a2a2";
    public static final List<String> RESOURCES = Arrays.asList("css/style.css", "js/main.js", "js/runtime.js");
    public static final String RESOURCE_BASE_PATH = "widgets/" + WIDGET_1_CODE + "/static";
    public static final List<String> JS_RESOURCES = Arrays.asList(RESOURCE_BASE_PATH + "/js/main.js",
            RESOURCE_BASE_PATH + "/js/runtime.js");
    public static final List<String> CSS_RESOURCES = Arrays.asList(RESOURCE_BASE_PATH + "/css/style.css");
    public static final String PLUGIN_INGRESS_1_CODE = API_CLAIM_1_SERVICE_ID;
    public static final String PLUGIN_INGRESS_1_PATH = "/" + API_CLAIM_1_SERVICE_ID + "/path";
    public static final String PLUGIN_INGRESS_2_CODE = API_CLAIM_2_SERVICE_ID;
    public static final String PLUGIN_INGRESS_2_PATH = "/" + API_CLAIM_2_SERVICE_ID + "/path";
    public static final String PLUGIN_INGRESS_3_CODE = "customBaseNameV5";
    public static final String PLUGIN_INGRESS_3_PATH = "/" + API_CLAIM_2_SERVICE_ID + "/path";
    public static final String PARENT_NAME = "parent-name";


    public static List<WidgetConfigurationDescriptor> stubWidgetConfigurationDescriptor() {
        return Collections.singletonList(
                WidgetConfigurationDescriptor.builder()
                        .pos(WIDGET_1_POS)
                        .code(WIDGET_1_CODE)
                        .build());
    }

    public static WidgetDescriptor stubWidgetDescriptorV1() {
        return stubWidgetDescriptor()
                .customUiPath(CUSTOM_UI_PATH)
                .configUi(stubConfigUiDescriptor())
                .descriptorMetadata(stubDescriptorMetadata())
                .build();
    }

    public static WidgetDescriptor stubWidgetDescriptorV5() {
        WidgetDescriptor widgetDescriptor = stubWidgetDescriptor()
                .code(null)
                .type(ComponentType.WIDGET.getTypeName())
                .name(WIDGET_1_NAME)
                .customElement(WIDGET_1_CODE)
                .configWidget(CONFIG_WIDGET)
                .apiClaims(stubApiClaims())
                .contextParams(List.of(
                        "page_code",
                        "info_startLang",
                        "systemParam_applicationBaseURL"
                ))
                .params(List.of(
                        new Param("paramA", "descA"),
                        new Param("paramB", "descB")
                ))
                .descriptorMetadata(stubDescriptorMetadata())
                .build();
        widgetDescriptor.setDescriptorVersion(DescriptorVersion.V5.getVersion());
        return widgetDescriptor;
    }

    public static WidgetDescriptorBuilder stubWidgetDescriptor() {
        return WidgetDescriptor.builder()
                .code(WIDGET_1_CODE)
                .titles(TITLES_MAP)
                .group(GROUP)
                .descriptorMetadata(DescriptorMetadata.builder().bundleCode(BUNDLE_CODE).build());
    }

    public static DescriptorMetadata stubDescriptorMetadata() {
        return DescriptorMetadata.builder().pluginIngressPathMap(stubPluginIngressPathMap())
                .filename(stubWidgetFilename()).bundleCode(BUNDLE_CODE)
                .templateGeneratorService(new WidgetTemplateGeneratorServiceDouble()).build();
    }

    private static String stubWidgetFilename() {
        return "stub-widget";
    }

    public static ConfigUi stubConfigUiDescriptor() {
        ConfigUi configUI = new ConfigUi();
        configUI.setCustomElement(CUSTOM_ELEMENT);
        configUI.setResources(RESOURCES);
        return configUI;
    }

    public static Map<String, String> stubPluginIngressPathMap() {
        return Map.of(
                PLUGIN_INGRESS_1_CODE, PLUGIN_INGRESS_1_PATH,
                PLUGIN_INGRESS_2_CODE, PLUGIN_INGRESS_2_PATH,
                PLUGIN_INGRESS_3_CODE, PLUGIN_INGRESS_3_PATH
        );
    }

    public static List<ApiClaim> stubApiClaims() {
        return Arrays.asList(new ApiClaim(API_CLAIM_1_NAME, API_CLAIM_1_TYPE, API_CLAIM_1_SERVICE_ID, null),
                new ApiClaim(API_CLAIM_2_NAME, API_CLAIM_2_TYPE, API_CLAIM_2_SERVICE_ID, API_CLAIM_2_BUNDLE_ID));
    }
}
