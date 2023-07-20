package org.entando.kubernetes.client.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.entando.kubernetes.client.model.EntandoCoreComponentDeleteResponse.EntandoCoreComponentDelete;
import org.entando.kubernetes.client.model.EntandoCoreComponentDeleteResponse.EntandoCoreComponentDeleteStatus;
import org.entando.kubernetes.model.bundle.ComponentType;
import org.entando.kubernetes.model.entandocore.EntandoCoreComponentUsage;
import org.entando.kubernetes.model.entandocore.EntandoCoreComponentUsageRequest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class EntandoCoreComponentTypeDeserializerTest {

    private ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldDeserialize() throws Exception {

        String deleteRequestJson = "{\"type\":\"pageModel\",\"code\":\"123\"}";
        EntandoCoreComponentDeleteRequest deleteRequest = objectMapper.readValue(deleteRequestJson,
                EntandoCoreComponentDeleteRequest.class);
        assertThat(deleteRequest.getType()).isEqualTo(ComponentType.PAGE_TEMPLATE);

        String deleteComponentJson = "{\"type\":\"pageModel\",\"code\":\"123\",\"status\":\"failure\"}";
        EntandoCoreComponentDelete componentToDelete = objectMapper.readValue(deleteComponentJson,
                EntandoCoreComponentDelete.class);
        assertThat(componentToDelete.getType()).isEqualTo(ComponentType.PAGE_TEMPLATE);
        assertThat(componentToDelete.getStatus()).isEqualTo(EntandoCoreComponentDeleteStatus.FAILURE);

        String usageRequestJson = "{\"type\":\"pageModel\",\"code\":\"123\"}";
        EntandoCoreComponentUsageRequest usageRequest = objectMapper.readValue(usageRequestJson,
                EntandoCoreComponentUsageRequest.class);
        assertThat(usageRequest.getType()).isEqualTo(ComponentType.PAGE_TEMPLATE);

        String usageComponentJson =
                "{\"type\":\"pageModel\",\"code\":\"123\",\"exist\":false,\"usage\":0,\"references\":[]}";
        EntandoCoreComponentUsage usageComponent = objectMapper.readValue(usageComponentJson,
                EntandoCoreComponentUsage.class);
        assertThat(usageComponent.getType()).isEqualTo(ComponentType.PAGE_TEMPLATE);

    }
}
