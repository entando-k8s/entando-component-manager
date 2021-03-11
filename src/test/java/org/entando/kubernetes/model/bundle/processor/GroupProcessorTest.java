package org.entando.kubernetes.model.bundle.processor;

import org.entando.kubernetes.client.EntandoCoreClientTestDouble;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class GroupProcessorTest extends BaseProcessorTest {

    @Test
    void shouldReturnMeaningfulErrorIfExceptionAriseDuringProcessing() {

        super.shouldReturnMeaningfulErrorIfExceptionAriseDuringProcessing(
                new GroupProcessor(new EntandoCoreClientTestDouble()), "group");
    }
}
