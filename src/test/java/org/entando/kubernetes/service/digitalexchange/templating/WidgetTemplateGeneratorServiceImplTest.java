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
import java.util.Optional;
import org.apache.commons.io.FileUtils;
import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.model.bundle.BundleType;
import org.entando.kubernetes.model.bundle.descriptor.BundleDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.widget.WidgetDescriptor;
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

    private PluginDataEntity extApiDataEntity = new PluginDataEntity()
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
        when(bundleReader.getBundleCode()).thenReturn(BundleStubHelper.BUNDLE_CODE);
        when(bundleReader.readBundleDescriptor()).thenReturn(bundleDescriptor);
        when(bundleDescriptor.getBundleType()).thenReturn(BundleType.STANDARD_BUNDLE);

        String expected = ("<script src=\"<@wp.resourceURL />/bundles/my-component-[REP]/widgets/my-code-"
                + "[REP]/static/js/main.js\"></script>\n"
                + "<script src=\"<@wp.resourceURL />/bundles/my-component-[REP]/widgets/my-code-"
                + "[REP]/static/js/runtime.js\"></script>\n\n"
                + "<link href=\"<@wp.resourceURL />/bundles/my-component-[REP]/widgets/my-code-"
                + "[REP]/static/css/style.css\" rel=\"stylesheet\">")
                .replace("[REP]", BundleInfoStubHelper.GIT_REPO_ADDRESS_8_CHARS_SHA);

        final String resourceTags = service.createResourceTags(descriptor.getCode(), bundleReader);
        assertThat(resourceTags).isEqualTo(expected);
    }

    @Test
    void shouldThrowExceptionIfCantProvideApiPathWhileCreatingTheAssignTags() {
        WidgetDescriptor descriptor = WidgetStubHelper.stubWidgetDescriptorV5();
        descriptor.getApiClaims().get(1).setPluginCode("non-existing");
        assertThrows(EntandoComponentManagerException.class, () -> service.createAssignTag(descriptor));
    }

    @Test
    void shouldCreateTheExpectedCustomElementTag() {
        String tag = service.createCustomElementTag(descriptor);
        assertThat(tag).isEqualTo("<" + WidgetStubHelper.WIDGET_1_CODE + " config=\"${mfeSystemConfig}\"/>");
    }

    @Test
    void shouldCreateTheExpectedMfeSystemConfigAssignTag() throws JsonProcessingException {
        when(repository.findByBundleCodeAndPluginCode(WidgetStubHelper.API_CLAIM_2_BUNDLE_ID,
                WidgetStubHelper.API_CLAIM_2_SERVICE_ID)).thenReturn(Optional.of(extApiDataEntity));

        final String assignTag = service.createAssignTag(descriptor);
        assertThat(assignTag).isEqualTo(
                "<#assign mfeSystemConfig>{'systemParams':{'api':{'int-api':{'url':'${systemParam_application"
                        + "BaseURL}/service-id-1/path'},'ext-api':{'url':'${systemParam_applicationBaseURL}/"
                        + "service-id-2/path'}}}}</#assign>");
    }

    @Test
    void shouldThrowExceptionWhileCreatingMfeSystemConfigForNonExistingPaths() {
        assertThrows(EntandoComponentManagerException.class, () -> service.createAssignTag(descriptor));
    }

    @Test
    void shouldGenerateTheExpectedWidgetTemplate() throws IOException {

        when(bundleReader.getWidgetResourcesOfType(anyString(), eq("js"))).thenReturn(
                WidgetStubHelper.JS_RESOURCES);
        when(bundleReader.getWidgetResourcesOfType(anyString(), eq("css"))).thenReturn(
                WidgetStubHelper.CSS_RESOURCES);

        when(bundleReader.getBundleUrl()).thenReturn(BundleInfoStubHelper.GIT_REPO_ADDRESS);
        BundleDescriptor bundleDescriptor = mock(BundleDescriptor.class);
        when(bundleReader.getBundleCode()).thenReturn(BundleStubHelper.BUNDLE_CODE);
        when(bundleReader.readBundleDescriptor()).thenReturn(bundleDescriptor);
        when(bundleDescriptor.getBundleType()).thenReturn(BundleType.STANDARD_BUNDLE);

        when(repository.findByBundleCodeAndPluginCode(WidgetStubHelper.API_CLAIM_2_BUNDLE_ID,
                WidgetStubHelper.API_CLAIM_2_SERVICE_ID)).thenReturn(Optional.of(extApiDataEntity));

        File file = new File("src/test/resources/widget.ftl");
        String expected = FileUtils.readFileToString(file, "UTF-8").trim();

        final String ftl = service.generateWidgetTemplate("any-path", descriptor, bundleReader);

        assertThat(ftl).isEqualTo(expected);
    }
}
