package org.entando.kubernetes.client.model.usage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.Optional;
import org.entando.kubernetes.model.bundle.ComponentType;
import org.entando.kubernetes.model.bundle.usage.ComponentUsage;
import org.entando.kubernetes.model.bundle.usage.ComponentUsage.ComponentReference;
import org.entando.kubernetes.model.bundle.usage.ComponentUsage.ComponentReferenceType;
import org.entando.kubernetes.model.entandocore.EntandoCoreComponentUsage;
import org.entando.kubernetes.model.entandocore.EntandoCoreComponentUsage.EntandoCoreComponentReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class ComponentUsageTest {

    private ObjectMapper mapper;

    @BeforeEach
    void init() {
        mapper = new ObjectMapper();
    }

    @Test
    void shouldIgnoreNullValuesForOnlineFieldWhenWritingToJson()
            throws JsonProcessingException {
        // Given
        ComponentUsage componentUsage =
                new ComponentUsage(ComponentType.WIDGET, "my-widget", true, 1,
                        false,
                        Arrays.asList(
                                new ComponentReference(ComponentType.WIDGET,
                                        ComponentReferenceType.INTERNAL,
                                        "my-widget",
                                        null),
                                new ComponentReference(ComponentType.PAGE,
                                        ComponentReferenceType.EXTERNAL,
                                        "page123",
                                        true)));
        String expectedJson = "{"
                + "\"type\":\"WIDGET\","
                + "\"code\":\"my-widget\","
                + "\"exist\":true,"
                + "\"usage\":1,"
                + "\"hasExternal\":false,"
                + "\"references\":[{"
                + "\"componentType\":\"WIDGET\","
                + "\"referenceType\":\"INTERNAL\","
                + "\"code\":\"my-widget\""
                + "},"
                + "{"
                + "\"componentType\":\"PAGE\","
                + "\"referenceType\":\"EXTERNAL\","
                + "\"code\":\"page123\","
                + "\"online\":true"
                + "}]"
                + "}";

        // When
        String dtoAsString = mapper.writeValueAsString(componentUsage);
        // Then
        assertEquals(expectedJson, dtoAsString);

    }

    @Test
    void shouldReturnOptionalEmptyOnMappingNullValuesFromEntandoCore() {
        // Given
        // When
        Optional<ComponentUsage> componentUsage = ComponentUsage.fromEntandoCore(null);
        Optional<ComponentReference> componentReference = ComponentReference.fromEntandoCore(null);
        // Then
        assertTrue(componentUsage.isEmpty());
        assertTrue(componentReference.isEmpty());

    }

    @Test
    void shouldSkipNullReferenceWhenMappingComponentUsage() {
        // Given
        EntandoCoreComponentUsage entandoCoreComponentUsage = new EntandoCoreComponentUsage();
        EntandoCoreComponentReference entandoCoreComponentReference = new EntandoCoreComponentReference();
        entandoCoreComponentUsage.setReferences(Arrays.asList(entandoCoreComponentReference, null));
        // When
        Optional<ComponentUsage> componentUsage = ComponentUsage.fromEntandoCore(entandoCoreComponentUsage);
        // Then
        assertTrue(componentUsage.isPresent());
        assertEquals(1, componentUsage.get().getReferences().size());
    }

}
