package org.entando.kubernetes.service.digitalexchange.component;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.entando.kubernetes.client.core.EntandoCoreClient;
import org.entando.kubernetes.model.bundle.ComponentType;
import org.entando.kubernetes.model.bundle.usage.ComponentUsage;
import org.entando.kubernetes.model.bundle.usage.ComponentUsage.ComponentReference;
import org.entando.kubernetes.model.bundle.usage.ComponentUsage.ComponentReferenceType;
import org.entando.kubernetes.model.entandocore.EntandoCoreComponentUsage;
import org.entando.kubernetes.model.entandocore.EntandoCoreComponentUsage.EntandoCoreComponentReference;
import org.entando.kubernetes.model.entandocore.EntandoCoreComponentUsage.IrrelevantComponentUsage;
import org.entando.kubernetes.model.job.EntandoBundleComponentJobEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

@Tag("in-process")
class EntandoBundleComponentUsageServiceTest {

    private EntandoBundleComponentUsageService usageService;
    private EntandoCoreClient client;

    @BeforeEach
    public void setup() {
        client = Mockito.mock(EntandoCoreClient.class);
        this.usageService = new EntandoBundleComponentUsageService(client);
    }

    @Test
    void shouldReturnComponentUsageForValidComponent() {
        when(client.getWidgetUsage("my-widget"))
                .thenReturn(
                        new EntandoCoreComponentUsage(ComponentType.WIDGET, "my-widget", true, 1, Collections.emptyList()));
        EntandoCoreComponentUsage cu = this.usageService.getUsage(ComponentType.WIDGET, "my-widget");
        assertThat(cu.getCode()).isEqualTo("my-widget");
        assertThat(cu.getUsage()).isEqualTo(1);
    }

    @Test
    void shouldReturnIrrelevantUsageInformation() {
        EntandoCoreComponentUsage cu = this.usageService.getUsage(ComponentType.LABEL, "my-great-label");
        assertThat(cu).isInstanceOf(IrrelevantComponentUsage.class);
        assertThat(cu.getType()).isEqualTo(ComponentType.LABEL);
        assertThat(cu.getCode()).isEqualTo("my-great-label");
        assertThat(cu.getUsage()).isZero();
    }


    @ParameterizedTest
    @MethodSource("provideComponentUsageDetailsAndExpectedResult")
    void shouldComponentUsageDetailsReturnCorrectData(
            List<EntandoCoreComponentUsage> entandoCoreComponentUsageList,
            Supplier<List<EntandoBundleComponentJobEntity>> componentJob,
            List<ComponentReferenceType> expectedReferenceTypes,
            int expectedUsage,
            boolean hasExternal) {
        // Given
        when(client.getComponentsUsageDetails(anyList()))
                .thenReturn(entandoCoreComponentUsageList);
        // When
        List<ComponentUsage> cuList = this.usageService.getComponentsUsageDetails(
                componentJob.get());
        // Then
        assertThat(cuList).hasSize(1);
        // the use list of the i-th component calculated by the service must contain the same number of elements
        // provided by Entando Core, except null references which are filtered by EntandoBundleComponentUsageService
        assertThat(cuList.get(0).getReferences()).hasSize(entandoCoreComponentUsageList.get(0).getReferences().size());
        IntStream.range(0, cuList.get(0).getReferences().size()).forEach(idx -> {
            ComponentReference computedComponentReference = cuList.get(0).getReferences().get(idx);
            EntandoCoreComponentReference coreComponentReference = entandoCoreComponentUsageList.get(0).getReferences()
                    .get(idx);
            // the type of the reference must be INTERNAL if the installed bundle components contains the reference,
            // EXTERNAL otherwise. The value is determined, by construction, in the test inputs
            assertThat(computedComponentReference.getReferenceType()).isEqualTo(expectedReferenceTypes.get(idx));
            // the online value must be reported unchanged as received from Entando Core for each reference
            assertThat(computedComponentReference.getOnline()).isEqualTo(coreComponentReference.getOnline());
        });
        // the expected usage should match the received one and the reference number in the computed list must mach with
        // this number
        assertEquals(expectedUsage,cuList.get(0).getUsage());
        assertEquals(expectedUsage,cuList.get(0).getReferences().size());
        // the boolean field 'hasExternal' must be set to true if at least one reference with a value equal to EXTERNAL
        // exists in the list of references
        assertEquals(hasExternal, cuList.get(0).getHasExternal());
    }

    /*
     * Utility method providing mock inputs (involved in a hypothetical scenario of interaction, relate to usage details,
     * between EntandoBundleComponentUsageService and EntandoCoreClient) and expected values of related test scenarios
     */
    private static Stream<Arguments> provideComponentUsageDetailsAndExpectedResult() {
        return Stream.of(
                Arguments.of(
                        // test inputs
                        Collections.singletonList(
                                new EntandoCoreComponentUsage(ComponentType.WIDGET, "my-widget", true, 1,
                                        List.of(
                                                new EntandoCoreComponentReference(ComponentType.WIDGET,
                                                        "my-widget",
                                                        null)))),
                        // provides an entity with the component id that matches the component reference code returned
                        // by the Entando Core, so we expect to have only one internal reference
                        (Supplier<List<EntandoBundleComponentJobEntity>>) () -> {
                            EntandoBundleComponentJobEntity componentJob = new EntandoBundleComponentJobEntity();
                            componentJob.setComponentId("my-widget");
                            componentJob.setComponentType(ComponentType.WIDGET);
                            return Collections.singletonList(componentJob);
                        },
                        // expected values
                        // type of reference expected
                        List.of(ComponentReferenceType.INTERNAL),
                        // expected usage
                        1,
                        // has external
                        false),
                Arguments.of(
                        // test inputs
                        Collections.singletonList(
                                new EntandoCoreComponentUsage(ComponentType.WIDGET, "my-widget", true, 1,
                                        List.of(
                                                new EntandoCoreComponentReference(ComponentType.WIDGET,
                                                        "my-widget",
                                                        null)))),
                        // no component job entity found, so we expect the provided reference to be external
                        (Supplier<List<EntandoBundleComponentJobEntity>>) Collections::emptyList,
                        // expected values
                        // type of reference expected
                        List.of(ComponentReferenceType.EXTERNAL),
                        // expected usage
                        1,
                        // has external
                        true),
                Arguments.of(
                        // test inputs
                        Collections.singletonList(
                                new EntandoCoreComponentUsage(ComponentType.WIDGET, "my-widget", true, 2,
                                        Arrays.asList(
                                                new EntandoCoreComponentReference(ComponentType.WIDGET,
                                                        "my-widget",
                                                        null),
                                                new EntandoCoreComponentReference(ComponentType.PAGE,
                                                        "page123",
                                                        true)))),
                        // provides only one entity out of 2 with a component id that matches the reference code. So we
                        // expect that the hasExternal value is true.  So we expect a reference with a type equal to
                        // INTERNAL and a reference with a type equal to EXTERNAL
                        (Supplier<List<EntandoBundleComponentJobEntity>>) () -> {
                            EntandoBundleComponentJobEntity componentJob = new EntandoBundleComponentJobEntity();
                            componentJob.setComponentId("my-widget");
                            componentJob.setComponentType(ComponentType.WIDGET);
                            return Collections.singletonList(componentJob);
                        },
                        // expected values
                        // type of reference expected
                        Arrays.asList(ComponentReferenceType.INTERNAL, ComponentReferenceType.EXTERNAL),
                        // expected usage
                        2,
                        // has external
                        true),
                Arguments.of(
                        // test inputs
                        Collections.singletonList(
                                new EntandoCoreComponentUsage(ComponentType.WIDGET, "my-widget", true, 2,
                                        Arrays.asList(
                                                new EntandoCoreComponentReference(ComponentType.WIDGET,
                                                        "my-widget",
                                                        null),
                                                new EntandoCoreComponentReference(ComponentType.PAGE,
                                                        "page123",
                                                        true)))),
                        // provides both entities with a component id that matches the returned reference codes.
                        // So we expect for both references a type equal to INTERNAL and that the hasExternal value
                        // is false
                        (Supplier<List<EntandoBundleComponentJobEntity>>) () -> {
                            EntandoBundleComponentJobEntity widgetComponentJob = new EntandoBundleComponentJobEntity();
                            widgetComponentJob.setComponentId("my-widget");
                            widgetComponentJob.setComponentType(ComponentType.WIDGET);
                            EntandoBundleComponentJobEntity pageComponentJob = new EntandoBundleComponentJobEntity();
                            pageComponentJob.setComponentId("page123");
                            pageComponentJob.setComponentType(ComponentType.PAGE);
                            return Arrays.asList(widgetComponentJob, pageComponentJob);
                        },
                        // expected values
                        Arrays.asList(ComponentReferenceType.INTERNAL, ComponentReferenceType.INTERNAL),
                        2,
                        false),
                Arguments.of(
                        // test inputs
                        Collections.singletonList(
                                // given that a bundle exist and the component inside it has no references
                                // then we expect a usage value equals to 0 and an empty references list
                                new EntandoCoreComponentUsage(ComponentType.PAGE, "page-1", true, 0,
                                        List.of())),
                        (Supplier<List<EntandoBundleComponentJobEntity>>) () -> {
                            EntandoBundleComponentJobEntity pageComponentJob = new EntandoBundleComponentJobEntity();
                            pageComponentJob.setComponentId("page-1");
                            pageComponentJob.setComponentType(ComponentType.PAGE);
                            return List.of(pageComponentJob);
                        },
                        // expected values
                        // type of reference expected
                        List.of(),
                        // expected usage
                        0,
                        // has external
                        false)
        );
    }
}
