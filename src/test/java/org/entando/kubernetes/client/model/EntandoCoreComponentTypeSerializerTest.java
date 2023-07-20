package org.entando.kubernetes.client.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import org.entando.kubernetes.client.model.EntandoCoreComponentDeleteResponse.EntandoCoreComponentDelete;
import org.entando.kubernetes.client.model.EntandoCoreComponentDeleteResponse.EntandoCoreComponentDeleteStatus;
import org.entando.kubernetes.model.bundle.ComponentType;
import org.entando.kubernetes.model.entandocore.EntandoCoreComponentUsage;
import org.entando.kubernetes.model.entandocore.EntandoCoreComponentUsageRequest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class EntandoCoreComponentTypeSerializerTest {

    private ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldSerialize() throws Exception {
        EntandoCoreComponentDeleteRequest deleteRequest = EntandoCoreComponentDeleteRequest
                .builder().type(ComponentType.PAGE_TEMPLATE).code("123").build();
        String deleteRequestJson = objectMapper.writeValueAsString(deleteRequest);
        System.out.println(deleteRequestJson);
        assertThat(deleteRequestJson).contains("pageModel");

        EntandoCoreComponentDelete componentToDelete = new EntandoCoreComponentDelete(
                ComponentType.PAGE_TEMPLATE,
                "123",
                EntandoCoreComponentDeleteStatus.SUCCESS
        );
        String deleteComponentJson = objectMapper.writeValueAsString(componentToDelete);
        System.out.println(deleteComponentJson);
        assertThat(deleteComponentJson).contains("pageModel");
        assertThat(deleteComponentJson).contains("success");

        EntandoCoreComponentUsageRequest usageRequest = new EntandoCoreComponentUsageRequest(
                ComponentType.PAGE_TEMPLATE,
                "123"
        );
        String usageRequestJson = objectMapper.writeValueAsString(usageRequest);
        System.out.println(usageRequestJson);
        assertThat(usageRequestJson).contains("pageModel");

        EntandoCoreComponentUsage usageComponent = new EntandoCoreComponentUsage(
                ComponentType.PAGE_TEMPLATE,
                "123",
                false,
                0,
                Collections.emptyList()
        );
        String usageComponentJson = objectMapper.writeValueAsString(usageComponent);
        System.out.println(usageComponentJson);
        assertThat(usageComponentJson).contains("pageModel");
    }

}
