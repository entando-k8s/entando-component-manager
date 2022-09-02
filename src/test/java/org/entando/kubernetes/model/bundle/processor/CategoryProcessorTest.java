package org.entando.kubernetes.model.bundle.processor;

import static java.util.Collections.singletonList;
import static org.mockito.Mockito.when;

import java.io.IOException;
import org.entando.kubernetes.client.EntandoCoreClientTestDouble;
import org.entando.kubernetes.model.bundle.BundleType;
import org.entando.kubernetes.model.bundle.descriptor.BundleDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.ComponentSpecDescriptor;
import org.entando.kubernetes.stubhelper.BundleStubHelper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class CategoryProcessorTest extends BaseProcessorTest {


    @Test
    void shouldReturnMeaningfulErrorIfExceptionAriseDuringProcessing() throws IOException {
        final ComponentSpecDescriptor spec = new ComponentSpecDescriptor();
        spec.setCategories(singletonList("categories/notexist.yaml"));
        BundleDescriptor bundleDescriptor = BundleStubHelper.stubBundleDescriptor(spec, BundleType.STANDARD_BUNDLE);

        when(baseBundleReader.readBundleDescriptor()).thenReturn(bundleDescriptor);

        super.shouldReturnMeaningfulErrorIfExceptionAriseDuringProcessing(
                new CategoryProcessor(new EntandoCoreClientTestDouble()), "category");

    }


}
