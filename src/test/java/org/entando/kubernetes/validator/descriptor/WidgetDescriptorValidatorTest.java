package org.entando.kubernetes.validator.descriptor;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;
import org.entando.kubernetes.exception.digitalexchange.InvalidBundleException;
import org.entando.kubernetes.model.bundle.descriptor.DescriptorVersion;
import org.entando.kubernetes.model.bundle.descriptor.widget.WidgetDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.widget.WidgetDescriptor.ApiClaim;
import org.entando.kubernetes.model.bundle.descriptor.widget.WidgetDescriptor.ConfigUi;
import org.entando.kubernetes.stubhelper.WidgetStubHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class WidgetDescriptorValidatorTest {

    private WidgetDescriptorValidator validator;
    private final YAMLMapper yamlMapper = new YAMLMapper();

    @BeforeEach
    public void genericSetup() {
        validator = new WidgetDescriptorValidator();
        validator.setupValidatorConfiguration();
    }

    @Test
    void shouldCorrectlyValidateWidgetDescriptorV1() throws IOException {
        WidgetDescriptor descriptor = yamlMapper
                .readValue(new File("src/test/resources/bundle/widgets/my_widget_descriptor.yaml"),
                        WidgetDescriptor.class);
        assertDoesNotThrow(() -> validator.validateOrThrow(descriptor));
    }

    @Test
    void shouldThrowExceptionWhileValidatingAnInvalidWidgetDescriptorV1() throws IOException {
        WidgetDescriptor descriptor = yamlMapper
                .readValue(new File("src/test/resources/bundle-v5/widgets/my_widget_descriptor_v5.yaml"),
                        WidgetDescriptor.class);
        descriptor.setDescriptorVersion(DescriptorVersion.V1.getVersion());
        assertThrows(InvalidBundleException.class, () -> validator.validateOrThrow(descriptor));
    }

    @Test
    void shouldThrowExceptionWhileValidatingAWidgetDescriptorV1NOTContainingTheExpectedFields() throws IOException {
        WidgetDescriptor descriptor = yamlMapper
                .readValue(new File("src/test/resources/bundle/widgets/my_widget_descriptor.yaml"),
                        WidgetDescriptor.class);
        assertDoesNotThrow(() -> validator.validateOrThrow(descriptor));

        descriptor.setCode("");
        assertThrows(InvalidBundleException.class, () -> validator.validateOrThrow(descriptor));

        descriptor.setCode("code");
        descriptor.setTitles(null);
        assertThrows(InvalidBundleException.class, () -> validator.validateOrThrow(descriptor));

        descriptor.setTitles(new HashMap());
        descriptor.setGroup(null);
        assertThrows(InvalidBundleException.class, () -> validator.validateOrThrow(descriptor));
    }

    @Test
    void shouldThrowExceptionWhileValidatingAWidgetDescriptorV1ContainingUnexpectedFields() throws IOException {
        WidgetDescriptor descriptor = yamlMapper
                .readValue(new File("src/test/resources/bundle/widgets/my_widget_descriptor.yaml"),
                        WidgetDescriptor.class);
        assertDoesNotThrow(() -> validator.validateOrThrow(descriptor));

        descriptor.setApiClaims(List.of(new ApiClaim()));
        assertThrows(InvalidBundleException.class, () -> validator.validateOrThrow(descriptor));

        descriptor.setApiClaims(null);
        descriptor.setConfigWidget("conf");
        assertThrows(InvalidBundleException.class, () -> validator.validateOrThrow(descriptor));

        descriptor.setConfigWidget(null);
        descriptor.setCustomElement("x-elem");
        assertThrows(InvalidBundleException.class, () -> validator.validateOrThrow(descriptor));

        descriptor.setCustomElement(null);
        descriptor.setName("name");
        assertThrows(InvalidBundleException.class, () -> validator.validateOrThrow(descriptor));
    }

    @Test
    void shouldCorrectlyValidateWidgetDescriptorV5() throws IOException {
        WidgetDescriptor descriptor = yamlMapper
                .readValue(new File("src/test/resources/bundle-v5/widgets/my_widget_descriptor_v5.yaml"),
                        WidgetDescriptor.class);
        assertDoesNotThrow(() -> validator.validateOrThrow(descriptor));
    }

    @Test
    void shouldCorrectlyValidateWidgetDescriptorV6() throws IOException {
        WidgetDescriptor descriptor = yamlMapper
                .readValue(new File("src/test/resources/bundle-v5/widgets/my_widget_descriptor_v6.yaml"),
                        WidgetDescriptor.class);
        assertDoesNotThrow(() -> validator.validateOrThrow(descriptor));
    }

    @Test
    void shouldThrowExceptionWhileValidatingAWidgetDescriptorV5OnwardNOTContainingTheExpectedFields() {
        Stream.of(
                "src/test/resources/bundle-v5/widgets/my_widget_descriptor_v5.yaml",
                 "src/test/resources/bundle-v5/widgets/my_widget_descriptor_v6.yaml"
        ).map(filePath -> {
            try {
                return yamlMapper.readValue(new File(filePath), WidgetDescriptor.class);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).forEach(widgetDescriptor -> {

            assertDoesNotThrow(() -> validator.validateOrThrow(widgetDescriptor));

            widgetDescriptor.setName(null);
            assertThrows(InvalidBundleException.class, () -> validator.validateOrThrow(widgetDescriptor));

            widgetDescriptor.setName("name");
            widgetDescriptor.setTitles(null);
            assertThrows(InvalidBundleException.class, () -> validator.validateOrThrow(widgetDescriptor));

            widgetDescriptor.setTitles(new HashMap());
            widgetDescriptor.setGroup(null);
            assertThrows(InvalidBundleException.class, () -> validator.validateOrThrow(widgetDescriptor));

            widgetDescriptor.setGroup("gree");
            widgetDescriptor.setCustomElement(null);
            assertThrows(InvalidBundleException.class, () -> validator.validateOrThrow(widgetDescriptor));
        });
    }

    @Test
    void shouldThrowExceptionWhileValidatingAWidgetDescriptorV5OnwardContainingUnexpectedFields() {
        Stream.of(
                "src/test/resources/bundle-v5/widgets/my_widget_descriptor_v5.yaml",
                "src/test/resources/bundle-v5/widgets/my_widget_descriptor_v6.yaml"
        ).map(filePath -> {
            try {
                return yamlMapper.readValue(new File(filePath), WidgetDescriptor.class);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).forEach(widgetDescriptor -> {

            assertDoesNotThrow(() -> validator.validateOrThrow(widgetDescriptor));

            widgetDescriptor.setConfigUi(new ConfigUi());
            assertThrows(InvalidBundleException.class, () -> validator.validateOrThrow(widgetDescriptor));

            widgetDescriptor.setConfigUi(null);

            widgetDescriptor.setCustomElement(null);
            widgetDescriptor.setCustomUiPath(null);
            widgetDescriptor.setCustomUi(null);
            assertThrows(InvalidBundleException.class, () -> validator.validateOrThrow(widgetDescriptor));

            widgetDescriptor.setParentCode("a-widget-code");
            widgetDescriptor.setParentName(null);
            assertThrows(InvalidBundleException.class, () -> validator.validateOrThrow(widgetDescriptor));
            widgetDescriptor.setParentCode("a-widget-code-12345678");
            assertDoesNotThrow(() -> validator.validateOrThrow(widgetDescriptor));
            widgetDescriptor.setParentCode(null);
            widgetDescriptor.setParentName("a-widget-name");
            assertDoesNotThrow(() -> validator.validateOrThrow(widgetDescriptor));
            widgetDescriptor.setParentCode(null);
            widgetDescriptor.setParentName(null);

            widgetDescriptor.setCustomElement(null);
            widgetDescriptor.setCustomUiPath("myuipath");
            widgetDescriptor.setCustomUi(null);
            assertDoesNotThrow(() -> validator.validateOrThrow(widgetDescriptor));

            widgetDescriptor.setCustomElement(null);
            widgetDescriptor.setCustomUiPath(null);
            widgetDescriptor.setCustomUi("myui");
            assertDoesNotThrow(() -> validator.validateOrThrow(widgetDescriptor));

            widgetDescriptor.setCustomElement("element");
            widgetDescriptor.setCustomUiPath("myuipath");
            widgetDescriptor.setCustomUi(null);
            assertThrows(InvalidBundleException.class, () -> validator.validateOrThrow(widgetDescriptor));

            widgetDescriptor.setCustomElement("element");
            widgetDescriptor.setCustomUiPath(null);
            widgetDescriptor.setCustomUi("myui");
            assertThrows(InvalidBundleException.class, () -> validator.validateOrThrow(widgetDescriptor));
        });


    }

    @Test
    void shouldThrowExceptionWhileValidatingAWidgetDescriptorWithInvalidApiClaims() {
        // internal api with bundle id
        WidgetDescriptor descriptor = WidgetStubHelper.stubWidgetDescriptorV5();
        descriptor.getApiClaims().get(0).setBundleId("id");
        assertThrows(InvalidBundleException.class, () -> validator.validateOrThrow(descriptor));

        // external api without bundle id
        descriptor.getApiClaims().get(0).setBundleId(null);
        descriptor.getApiClaims().get(1).setBundleId(null);
        assertThrows(InvalidBundleException.class, () -> validator.validateOrThrow(descriptor));
    }

    @Test
    void shouldThrowExceptionWhileValidatingAWidgetDescriptorWithParentNameAndParentCode() {
        WidgetDescriptor descriptor = WidgetStubHelper.stubWidgetDescriptorV5()
                .setParentName(WidgetStubHelper.PARENT_NAME)
                .setParentCode("code");

        assertThrows(InvalidBundleException.class, () -> validator.validateOrThrow(descriptor));
    }

    @Test
    void shouldThrowExceptionWhileValidatingAWidgetDescriptorWithParentCodeWithInvalidFormat() {
        WidgetDescriptor descriptor = WidgetStubHelper.stubWidgetDescriptorV5();
        descriptor.setParentCode("wrong!format");
        assertThrows(InvalidBundleException.class, () -> validator.validateOrThrow(descriptor));

        descriptor.setParentCode("a-good_format");
        assertThrows(InvalidBundleException.class, () -> validator.validateOrThrow(descriptor));

        descriptor.setParentCode("a-good_format-99999999");
        assertDoesNotThrow(() -> validator.validateOrThrow(descriptor));
    }
    
}
