package org.entando.kubernetes.client.model.usage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import org.entando.kubernetes.model.bundle.ComponentType;
import org.entando.kubernetes.model.bundle.usage.ComponentUsage;
import org.entando.kubernetes.model.bundle.usage.ComponentUsage.ComponentReference;
import org.entando.kubernetes.model.bundle.usage.ComponentUsage.ComponentReferenceType;
import org.junit.jupiter.api.Assertions;
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
                new ComponentUsage(ComponentType.WIDGET.getTypeName(), "my-widget", true, 1,
                        false,
                        Arrays.asList(
                                new ComponentReference(ComponentType.WIDGET.getTypeName(), ComponentReferenceType.INTERNAL,
                                        "my-widget",
                                        null),
                                new ComponentReference(ComponentType.PAGE.getTypeName(), ComponentReferenceType.EXTERNAL,
                                        "page123",
                                        true)));
        String expectedJson = "{"
                + "\"type\":\"widget\","
                + "\"code\":\"my-widget\","
                + "\"exist\":true,"
                + "\"usage\":1,"
                + "\"hasExternal\":false,"
                + "\"references\":[{"
                + "\"componentType\":\"widget\","
                + "\"referenceType\":\"INTERNAL\","
                + "\"code\":\"my-widget\""
                + "},"
                + "{"
                + "\"componentType\":\"page\","
                + "\"referenceType\":\"EXTERNAL\","
                + "\"code\":\"page123\","
                + "\"online\":true"
                + "}]"
                + "}";

        // When
        String dtoAsString = mapper.writeValueAsString(componentUsage);
        // Then
        Assertions.assertEquals(expectedJson, dtoAsString);

    }


}
