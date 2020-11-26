package org.entando.kubernetes.model.bundle.processor;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.model.bundle.ComponentType;
import org.entando.kubernetes.model.bundle.descriptor.ComponentSpecDescriptor;
import org.entando.kubernetes.model.bundle.reader.BundleReader;
import org.entando.kubernetes.stubhelper.ReportableStubHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class ComponentProcessorTest {

    private ContentComponentProcessorTestDouble componentProcessor;

    @Mock
    private ComponentSpecDescriptor componentSpecDescriptor;
    @Mock
    private BundleReader bundleReader;

    @BeforeEach
    void setup() {
        componentProcessor = new ContentComponentProcessorTestDouble();
    }

    @Test
    void receivingAValidaComponentTypeShouldSupportOnlyTheRigthComponent() {

        assertThat(componentProcessor.supportComponent(ComponentType.CONTENT)).isTrue();
        assertThat(componentProcessor.supportComponent(ComponentType.ASSET)).isFalse();
    }

    @Test
    void getComponentSelectionFnShouldReturnTheRightFunction() {

        List<String> expectedList = Arrays
                .asList(ReportableStubHelper.CONTENT_CODE_1, ReportableStubHelper.CONTENT_CODE_2);
        when(componentSpecDescriptor.getContents()).thenReturn(expectedList);

        assertThat(componentProcessor.getComponentSelectionFn().get().apply(componentSpecDescriptor))
                .containsExactly(expectedList.toArray(String[]::new));
    }

    @Test
    void getDescriptorListWithAnEmptyComponentSelectionFnShouldThrowEntandoComponentManagerException() {

        ContentComponentProcessorTestDouble noCompSelectionFnComponentProcessor = new ContentComponentProcessorTestDouble() {

            @Override
            public Optional<Function<ComponentSpecDescriptor, List<String>>> getComponentSelectionFn() {
                return Optional.empty();
            }
        };

        Assertions.assertThrows(EntandoComponentManagerException.class, () -> {
            noCompSelectionFnComponentProcessor.getDescriptorList(bundleReader);
        });
    }
}