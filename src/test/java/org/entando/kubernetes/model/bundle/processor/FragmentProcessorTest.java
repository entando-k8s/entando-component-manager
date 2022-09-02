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
class FragmentProcessorTest extends BaseProcessorTest {

    @Test
    void shouldReturnMeaningfulErrorIfExceptionAriseDuringProcessing() {
        final String fileName = "fragments/notexist.yaml";
        final ComponentSpecDescriptor spec = new ComponentSpecDescriptor();
        spec.setFragments(singletonList(fileName));

        super.shouldReturnMeaningfulErrorIfExceptionAriseDuringProcessing(
                new FragmentProcessor(new EntandoCoreClientTestDouble()), spec, fileName);
    }

}
