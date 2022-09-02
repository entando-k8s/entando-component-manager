package org.entando.kubernetes.model.bundle.processor;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.io.IOException;
import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.model.bundle.BundleType;
import org.entando.kubernetes.model.bundle.descriptor.BundleDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.ComponentSpecDescriptor;
import org.entando.kubernetes.model.bundle.reader.BundleReader;
import org.entando.kubernetes.stubhelper.BundleStubHelper;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BaseProcessorTest {

    @Mock
    protected BundleReader baseBundleReader;

    protected void shouldReturnMeaningfulErrorIfExceptionAriseDuringProcessing(BaseComponentProcessor<?> processor,
            String componentTypeName) {

        try {
            when(baseBundleReader.readListOfDescriptorFile(any(), any())).thenThrow(new IOException());
        } catch (Exception e) {
            fail(e.getMessage());
        }

        EntandoComponentManagerException thrown = assertThrows(
                EntandoComponentManagerException.class,
                () -> processor.process(baseBundleReader, null, null)
        );

        assertThat(thrown.getMessage()).isEqualTo("Error processing " + componentTypeName + " components");

    }

    protected void shouldReturnMeaningfulErrorIfExceptionAriseDuringProcessing(BaseComponentProcessor<?> processor,
            ComponentSpecDescriptor spec, String fileName) {

        final String errorMessage = "exception test";
        BundleDescriptor bundleDescriptor = BundleStubHelper.stubBundleDescriptor(spec, BundleType.STANDARD_BUNDLE);

        when(baseBundleReader.readBundleDescriptor()).thenReturn(bundleDescriptor);
        when(baseBundleReader.readDescriptorFile(eq(fileName), any())).thenThrow(
                new EntandoComponentManagerException(errorMessage));

        EntandoComponentManagerException thrown = assertThrows(
                EntandoComponentManagerException.class,
                () -> processor.process(baseBundleReader, null, null)
        );

        assertThat(thrown.getMessage()).isEqualTo(errorMessage);

    }

    protected void shouldReturnMeaningfulErrorIfExceptionAriseDuringProcessing(BaseComponentProcessor<?> processor,
            String componentTypeName,
            ComponentSpecDescriptor spec) {

        BundleDescriptor bundleDescriptor = BundleStubHelper.stubBundleDescriptor(spec, BundleType.STANDARD_BUNDLE);
        when(baseBundleReader.readBundleDescriptor()).thenReturn(bundleDescriptor);

        try {
            when(baseBundleReader.readListOfDescriptorFile(any(), any())).thenThrow(new IOException());
        } catch (Exception e) {
            fail(e.getMessage());
        }

        EntandoComponentManagerException thrown = assertThrows(
                EntandoComponentManagerException.class,
                () -> processor.process(baseBundleReader, null, null)
        );

        assertThat(thrown.getMessage()).isEqualTo("Error processing " + componentTypeName + " components");

    }

}
