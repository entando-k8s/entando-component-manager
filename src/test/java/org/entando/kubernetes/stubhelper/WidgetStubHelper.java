package org.entando.kubernetes.stubhelper;

import java.util.Collections;
import java.util.List;
import org.entando.kubernetes.model.bundle.descriptor.WidgetConfigurationDescriptor;

public class WidgetStubHelper {

    public static final int WIDGET_1_POS = 0;
    public static final String WIDGET_1_CODE = "my-code";


    public static List<WidgetConfigurationDescriptor> stubWidgetConfigurationDescriptor() {
        return Collections.singletonList(
                WidgetConfigurationDescriptor.builder()
                        .pos(WIDGET_1_POS)
                        .code(WIDGET_1_CODE)
                        .build());
    }
}
