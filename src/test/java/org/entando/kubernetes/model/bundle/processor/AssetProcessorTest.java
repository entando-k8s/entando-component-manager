package org.entando.kubernetes.model.bundle.processor;

import static java.util.Collections.singletonList;

import org.entando.kubernetes.client.EntandoCoreClientTestDouble;
import org.entando.kubernetes.model.bundle.descriptor.ComponentSpecDescriptor;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class AssetProcessorTest extends BaseProcessorTest {


    @Test
    void shouldReturnMeaningfulErrorIfExceptionAriseDuringProcessing() {
        final String fileName = "assets/notexist.yaml";
        final ComponentSpecDescriptor spec = new ComponentSpecDescriptor();
        spec.setAssets(singletonList(fileName));

        super.shouldReturnMeaningfulErrorIfExceptionAriseDuringProcessing(
                new AssetProcessor(new EntandoCoreClientTestDouble()), spec, fileName);
    }

}
