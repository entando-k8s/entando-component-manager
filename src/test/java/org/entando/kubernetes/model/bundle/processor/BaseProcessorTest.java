package org.entando.kubernetes.model.bundle.processor;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.when;

import java.io.IOException;
import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.model.bundle.reader.BundleReader;
import org.entando.kubernetes.stubhelper.BundleInfoStubHelper;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BaseProcessorTest {

    @Mock
    protected BundleReader baseBundleReader;

    protected void shouldReturnMeaningfulErrorIfExceptionAriseDuringProcessing(BaseComponentProcessor<?> processor,
            String componentTypeName) {

        try {
            when(baseBundleReader.readBundleDescriptor()).thenThrow(new IOException());
        } catch (Exception e) {
            fail(e.getMessage());
        }
        Mockito.lenient().when(baseBundleReader.getBundleUrl()).thenReturn(BundleInfoStubHelper.GIT_REPO_ADDRESS);

        EntandoComponentManagerException thrown = assertThrows(
                EntandoComponentManagerException.class,
                () -> processor.process(baseBundleReader, null, null)
        );

        assertThat(thrown.getMessage()).isEqualTo("Error processing " + componentTypeName + " components");

    }

}
