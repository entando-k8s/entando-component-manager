package org.entando.kubernetes.model.bundle.processor;

import static java.util.Collections.singletonList;

import java.io.IOException;
import org.entando.kubernetes.client.EntandoCoreClientTestDouble;
import org.entando.kubernetes.model.bundle.descriptor.ComponentSpecDescriptor;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class GroupProcessorTest extends BaseProcessorTest {

    @Test
    void shouldReturnMeaningfulErrorIfExceptionAriseDuringProcessing() throws IOException {

        final String fileName = "groups/notexist.yaml";
        final ComponentSpecDescriptor spec = new ComponentSpecDescriptor();
        spec.setGroups(singletonList(fileName));

        super.shouldReturnMeaningfulErrorIfExceptionAriseDuringProcessing(
                new GroupProcessor(new EntandoCoreClientTestDouble()), "group", spec);
    }
    
}
