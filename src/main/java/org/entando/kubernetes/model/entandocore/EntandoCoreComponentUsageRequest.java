package org.entando.kubernetes.model.entandocore;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.entando.kubernetes.client.model.EntandoCoreComponentTypeDeserializer;
import org.entando.kubernetes.client.model.EntandoCoreComponentTypeSerializer;
import org.entando.kubernetes.model.bundle.ComponentType;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EntandoCoreComponentUsageRequest {

    @JsonSerialize(using = EntandoCoreComponentTypeSerializer.class)
    @JsonDeserialize(using = EntandoCoreComponentTypeDeserializer.class)
    private ComponentType type;
    private String code;

}
