package org.entando.kubernetes.model.bundle.descriptor.widget;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.entando.kubernetes.model.bundle.reader.BundleReader;
import org.entando.kubernetes.stubhelper.BundleInfoStubHelper;
import org.entando.kubernetes.stubhelper.WidgetStubHelper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class WidgetDescriptorTest {

    @Mock
    private BundleReader bundleReader;

    @Test
    void shouldSetTheExpectedWidgetCodeWithWidgetDescriptorV1AndCodeWithoutTheHash() {
        when(bundleReader.getBundleUrl()).thenReturn(BundleInfoStubHelper.GIT_REPO_ADDRESS);

        final WidgetDescriptor widgetDescriptor = WidgetStubHelper.stubWidgetDescriptorV1();

        widgetDescriptor.setCode(bundleReader);

        assertThat(widgetDescriptor.getCode()).isEqualTo(WidgetStubHelper.WIDGET_1_CODE + "-"
                + BundleInfoStubHelper.GIT_REPO_ADDRESS_8_CHARS_SHA);
    }

    @Test
    void shouldSetTheExpectedWidgetCodeWithWidgetDescriptorV1AndCodeWithTheCorrectHash() {
        when(bundleReader.getBundleUrl()).thenReturn(BundleInfoStubHelper.GIT_REPO_ADDRESS);

        WidgetDescriptor widgetDescriptor = WidgetStubHelper.stubWidgetDescriptorV1();
        widgetDescriptor.setCode(WidgetStubHelper.WIDGET_1_CODE + "-"
                + BundleInfoStubHelper.GIT_REPO_ADDRESS_8_CHARS_SHA);

        widgetDescriptor.setCode(bundleReader);

        assertThat(widgetDescriptor.getCode()).isEqualTo(WidgetStubHelper.WIDGET_1_CODE + "-"
                + BundleInfoStubHelper.GIT_REPO_ADDRESS_8_CHARS_SHA);
    }

    @Test
    void shouldSetTheExpectedWidgetCodeWithWidgetDescriptorV1AndCodeWithAnIncorrectHash() {
        when(bundleReader.getBundleUrl()).thenReturn(BundleInfoStubHelper.GIT_REPO_ADDRESS);

        String wrongHash = "abcd1234";

        WidgetDescriptor widgetDescriptor = WidgetStubHelper.stubWidgetDescriptorV1();
        widgetDescriptor.setCode(WidgetStubHelper.WIDGET_1_CODE + "-" + wrongHash);

        widgetDescriptor.setCode(bundleReader);

        assertThat(widgetDescriptor.getCode()).isEqualTo(WidgetStubHelper.WIDGET_1_CODE + "-" + wrongHash
                + "-" + BundleInfoStubHelper.GIT_REPO_ADDRESS_8_CHARS_SHA);
    }

    @Test
    void shouldSetTheExpectedWidgetCodeWithWidgetDescriptorV5() {
        when(bundleReader.getBundleUrl()).thenReturn(BundleInfoStubHelper.GIT_REPO_ADDRESS);

        WidgetDescriptor widgetDescriptor = WidgetStubHelper.stubWidgetDescriptorV5();

        widgetDescriptor.setCode(bundleReader);

        assertThat(widgetDescriptor.getCode()).isEqualTo(WidgetStubHelper.WIDGET_1_NAME
                + "-" + BundleInfoStubHelper.GIT_REPO_ADDRESS_8_CHARS_SHA);
    }
}
