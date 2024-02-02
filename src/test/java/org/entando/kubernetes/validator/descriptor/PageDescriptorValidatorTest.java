package org.entando.kubernetes.validator.descriptor;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.entando.kubernetes.exception.digitalexchange.InvalidBundleException;
import org.entando.kubernetes.model.bundle.descriptor.DescriptorVersion;
import org.entando.kubernetes.model.bundle.descriptor.PageDescriptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class PageDescriptorValidatorTest {

    private PageDescriptorValidator validator;
    private final YAMLMapper yamlMapper = new YAMLMapper();

    @BeforeEach
    public void genericSetup() {
        validator = new PageDescriptorValidator();
        validator.setupValidatorConfiguration();
    }

    @Test
    void shouldCorrectlyValidateWidgetDescriptorV1_1() throws IOException {
        PageDescriptor descriptor = yamlMapper
                .readValue(new File("src/test/resources/bundle/pages/my_page_descriptor.yaml"),
                        PageDescriptor.class);
        assertDoesNotThrow(() -> validator.validateOrThrow(descriptor));
    }

    @Test
    void shouldCorrectlyValidateWidgetDescriptorV1_2() throws IOException {
        PageDescriptor descriptor = yamlMapper
                .readValue(new File("src/test/resources/bundle/pages/another_page_descriptor.yaml"),
                        PageDescriptor.class);
        assertDoesNotThrow(() -> validator.validateOrThrow(descriptor));
    }

    @Test
    void shouldCorrectlyValidateWidgetDescriptorV5_1() throws IOException {
        PageDescriptor descriptor = yamlMapper
                .readValue(new File("src/test/resources/bundle-v5/page/page_descriptor_v5.yaml"),
                        PageDescriptor.class);
        assertDoesNotThrow(() -> validator.validateOrThrow(descriptor));
    }

    @Test
    void shouldCorrectlyValidateWidgetDescriptorV6_1() throws IOException {
        PageDescriptor descriptor = yamlMapper
                .readValue(new File("src/test/resources/bundle-v5/page/page_descriptor_v6.yaml"),
                        PageDescriptor.class);
        assertDoesNotThrow(() -> validator.validateOrThrow(descriptor));
    }
    
    @Test
    void shouldThrowExceptionWhileValidatingAnInvalidWidgetDescriptorV1() throws IOException {
        PageDescriptor descriptor = yamlMapper
                .readValue(new File("src/test/resources/bundle-v5/page/page_descriptor_v5.yaml"),
                        PageDescriptor.class);
        descriptor.setDescriptorVersion(DescriptorVersion.V1.getVersion());
        assertThrows(InvalidBundleException.class, () -> validator.validateOrThrow(descriptor));
    }

    @Test
    void shouldCorrectlyValidateWidgetDescriptorV5_2() throws IOException {
        PageDescriptor descriptor = yamlMapper
                .readValue(new File("src/test/resources/bundle-v5/page/another_page_descriptor_v5.yaml"),
                        PageDescriptor.class);
        assertDoesNotThrow(() -> validator.validateOrThrow(descriptor));
        descriptor.setParentCode("my_homepage");
        assertThrows(InvalidBundleException.class, () -> validator.validateOrThrow(descriptor));
    }

    @Test
    void shouldCorrectlyValidateWidgetDescriptorV6_2() throws IOException {
        PageDescriptor descriptor = yamlMapper
                .readValue(new File("src/test/resources/bundle-v5/page/page_descriptor_v6.yaml"),
                        PageDescriptor.class);
        assertDoesNotThrow(() -> validator.validateOrThrow(descriptor));
        descriptor.setParentCode("my_homepage");
        assertThrows(InvalidBundleException.class, () -> validator.validateOrThrow(descriptor));
    }

    @Test
    void shouldThrowExceptionWhileValidatingADescriptorV5OnwardNOTContainingTheExpectedFields() throws IOException {
        PageDescriptor descriptorV5 = yamlMapper
                .readValue(new File("src/test/resources/bundle-v5/page/page_descriptor_v5.yaml"),
                        PageDescriptor.class);
        assertDoesNotThrow(() -> validator.validateOrThrow(descriptorV5));

        validateV5Onward(descriptorV5);

        PageDescriptor descriptorV6 = yamlMapper
                .readValue(new File("src/test/resources/bundle-v5/page/page_descriptor_v6.yaml"),
                        PageDescriptor.class);
        assertDoesNotThrow(() -> validator.validateOrThrow(descriptorV6));

        validateV5Onward(descriptorV6);

    }

    private void validateV5Onward(PageDescriptor descriptor) {
        descriptor.setCode("code");
        assertThrows(InvalidBundleException.class, () -> validator.validateOrThrow(descriptor));

        descriptor.setCode(null);
        descriptor.setTitles(null);
        assertThrows(InvalidBundleException.class, () -> validator.validateOrThrow(descriptor));

        Map<String, String> titles = new HashMap<>();
        titles.put("en", "En title");
        titles.put("it", "IT title");
        descriptor.setTitles(titles);
        descriptor.setOwnerGroup(null);
        assertThrows(InvalidBundleException.class, () -> validator.validateOrThrow(descriptor));

        descriptor.setOwnerGroup("group");
        descriptor.setParentCode("parentCode");
        assertThrows(InvalidBundleException.class, () -> validator.validateOrThrow(descriptor));

        descriptor.setParentName(null);
        assertThrows(InvalidBundleException.class, () -> validator.validateOrThrow(descriptor));

        descriptor.setParentCode("parentCode-12345678");
        assertDoesNotThrow(() -> validator.validateOrThrow(descriptor));
    }

}
