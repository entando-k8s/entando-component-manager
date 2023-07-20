package org.entando.kubernetes.client.model;

import static org.entando.kubernetes.model.bundle.ComponentType.ASSET;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.entando.kubernetes.model.bundle.ComponentType;
import org.entando.kubernetes.model.job.EntandoBundleComponentJobEntity;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntandoCoreComponentDeleteRequest {

    @JsonSerialize(using = EntandoCoreComponentTypeSerializer.class)
    @JsonDeserialize(using = EntandoCoreComponentTypeDeserializer.class)
    private ComponentType type;
    private String code;

    public static Optional<EntandoCoreComponentDeleteRequest> fromEntity(EntandoBundleComponentJobEntity entity) {
        return Optional.ofNullable(entity).map(e -> EntandoCoreComponentDeleteRequest.builder()
                .type(e.getComponentType())
                .code(mapCodeByType(e.getComponentType(), e.getComponentId()))
                .build());
    }

    private static String mapCodeByType(ComponentType type, String code) {
        if (ASSET.equals(type)) {
            return "cc=" + code;
        } else {
            return code;
        }
    }
}
