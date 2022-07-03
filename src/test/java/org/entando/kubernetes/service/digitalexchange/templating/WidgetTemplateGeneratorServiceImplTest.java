package org.entando.kubernetes.service.digitalexchange.templating;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import org.apache.commons.io.FileUtils;
import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.model.bundle.BundleType;
import org.entando.kubernetes.model.bundle.descriptor.BundleDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.widget.WidgetDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.widget.WidgetDescriptor.ApiClaim;
import org.entando.kubernetes.model.bundle.reader.BundleReader;
import org.entando.kubernetes.model.job.PluginDataEntity;
import org.entando.kubernetes.repository.PluginDataRepository;
import org.entando.kubernetes.stubhelper.BundleInfoStubHelper;
import org.entando.kubernetes.stubhelper.BundleStubHelper;
import org.entando.kubernetes.stubhelper.WidgetStubHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class WidgetTemplateGeneratorServiceImplTest {

    @Mock
    private BundleReader bundleReader;
    @Mock
    private PluginDataRepository repository;

    private WidgetDescriptor descriptor = WidgetStubHelper.stubWidgetDescriptorV5();
    private WidgetTemplateGeneratorServiceImpl service;

    private PluginDataEntity extApiDataEntity1 = new PluginDataEntity()
            .setEndpoint(WidgetStubHelper.PLUGIN_INGRESS_1_PATH);

    private PluginDataEntity extApiDataEntity2 = new PluginDataEntity()
            .setEndpoint(WidgetStubHelper.PLUGIN_INGRESS_2_PATH);

    @BeforeEach
    public void setup() {
        service = new WidgetTemplateGeneratorServiceImpl(repository);
    }

    @Test
    void shouldCreateTheExpectedResourceTags() throws IOException {

        when(bundleReader.getWidgetResourcesOfType(descriptor.getCode(), "js")).thenReturn(
                WidgetStubHelper.JS_RESOURCES);
        when(bundleReader.getWidgetResourcesOfType(descriptor.getCode(), "css")).thenReturn(
                WidgetStubHelper.CSS_RESOURCES);
        when(bundleReader.getBundleUrl()).thenReturn(BundleInfoStubHelper.GIT_REPO_ADDRESS);
        BundleDescriptor bundleDescriptor = mock(BundleDescriptor.class);
        when(bundleReader.getCode()).thenReturn(
                BundleStubHelper.BUNDLE_CODE + "-" + BundleInfoStubHelper.GIT_REPO_ADDRESS_8_CHARS_SHA);
        when(bundleReader.readBundleDescriptor()).thenReturn(bundleDescriptor);
        when(bundleDescriptor.getBundleType()).thenReturn(BundleType.STANDARD_BUNDLE);

        String expected = ("<script src=\"<@wp.resourceURL />bundles/my-component-[REP]/widgets/my-code-"
                + "[REP]/static/js/main.js\"></script>\n"
                + "<script src=\"<@wp.resourceURL />bundles/my-component-[REP]/widgets/my-code-"
                + "[REP]/static/js/runtime.js\"></script>\n\n"
                + "<link href=\"<@wp.resourceURL />bundles/my-component-[REP]/widgets/my-code-"
                + "[REP]/static/css/style.css\" rel=\"stylesheet\">")
                .replace("[REP]", BundleInfoStubHelper.GIT_REPO_ADDRESS_8_CHARS_SHA);

        final String resourceTags = service.generateCodeForResourcesInclusion(descriptor.getCode(), bundleReader);
        assertThat(resourceTags).isEqualTo(expected);
    }

    @Test
    void shouldThrowException_If_claimingApiToNonExistentExternal() {
        assertThrows(EntandoComponentManagerException.class, () -> service.checkApiClaim(
                new ApiClaim("api-ext", "external", "non-existing", "99999999"),
                "99999999"
        ));
    }

    @Test
    void shouldCreateTheExpectedCustomElementTag() {
        String tag = service.generateCodeForCustomElementInvocation(descriptor);
        assertThat(tag).isEqualTo("<" + WidgetStubHelper.WIDGET_1_CODE
                + " config=\"<#outputformat 'HTML'>${mfeConfig}</#outputformat>\"/>");
    }

    @Test
    void shouldProperlyGenerateMfeConfigObject() throws JsonProcessingException {
        var assignTag = service.generateCodeForMfeConfigObjectCreation(descriptor,
                WidgetStubHelper.API_CLAIM_1_BUNDLE_ID);

        assertThat(assignTag).isEqualTo("<#assign mfeConfig>"
                + "{\"systemParams\":{\"api\":{"
                + "\"int-api\":{\"url\":\"${apiClaim_int__DASH__api}\"},"
                + "\"ext-api\":{\"url\":\"${apiClaim_ext__DASH__api}\"}}},"
                + "\"contextParams\":{\"info_startLang\":\"${info_startLang}\",\"page_code\":\"${page_code}\","
                + "\"systemParam_applicationBaseURL\":\"${systemParam_applicationBaseURL}\"},"
                + "\"params\":{\"paramA\":\"${widget_paramA}\",\"paramB\":\"${widget_paramB}\"}}"
                + "</#assign>"
        );

        descriptor.setContextParams(new ArrayList<>());
        descriptor.setParams(new ArrayList<>());
        descriptor.setApiClaims(new ArrayList<>());
        assignTag = service.generateCodeForMfeConfigObjectCreation(descriptor, WidgetStubHelper.API_CLAIM_2_BUNDLE_ID);

        assertThat(assignTag).isEqualTo("<#assign mfeConfig>"
                + "{\"systemParams\":{\"api\":{}},"
                + "\"contextParams\":{},"
                + "\"params\":{}}"
                + "</#assign>"
        );
    }

    @Test
    void shouldThrowExceptionWhen_GeneratingApiVarAssignments_WithNonExistingPaths() {
        assertThrows(EntandoComponentManagerException.class,
                () -> service.updateWidgetTemplate("", descriptor.getApiClaims(), null, null));
    }

    @Test
    void shouldGenerateTheExpectedWidgetTemplate() throws IOException {

        when(bundleReader.getWidgetResourcesOfType(anyString(), eq("js"))).thenReturn(
                WidgetStubHelper.JS_RESOURCES);
        when(bundleReader.getWidgetResourcesOfType(anyString(), eq("css"))).thenReturn(
                WidgetStubHelper.CSS_RESOURCES);

        when(bundleReader.getBundleUrl()).thenReturn(BundleInfoStubHelper.GIT_REPO_ADDRESS);
        BundleDescriptor bundleDescriptor = mock(BundleDescriptor.class);
        when(bundleReader.getCode()).thenReturn(
                BundleStubHelper.BUNDLE_CODE + "-" + BundleInfoStubHelper.GIT_REPO_ADDRESS_8_CHARS_SHA);
        when(bundleReader.readBundleDescriptor()).thenReturn(bundleDescriptor);
        when(bundleReader.calculateBundleId()).thenCallRealMethod();
        when(bundleDescriptor.getBundleType()).thenReturn(BundleType.STANDARD_BUNDLE);

        File expectedOnFile = new File("src/test/resources/widget.ftl");
        String expected = FileUtils.readFileToString(expectedOnFile, "UTF-8").trim();

        final String ftl = service.generateWidgetTemplate("widgets/any-path", descriptor, bundleReader);

        assertThat(ftl).isEqualTo(expected);
    }
}
