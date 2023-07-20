package org.entando.kubernetes.model.entandocore;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.Collections;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.entando.kubernetes.client.model.EntandoCoreComponentTypeDeserializer;
import org.entando.kubernetes.client.model.EntandoCoreComponentTypeSerializer;
import org.entando.kubernetes.model.bundle.ComponentType;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class EntandoCoreComponentUsage {

    @JsonSerialize(using = EntandoCoreComponentTypeSerializer.class)
    @JsonDeserialize(using = EntandoCoreComponentTypeDeserializer.class)
    private ComponentType type;
    private String code;
    private boolean exist;
    private int usage;
    private List<EntandoCoreComponentReference> references;

    public static class NoUsageComponent extends EntandoCoreComponentUsage {

        public NoUsageComponent(ComponentType type) {
            super(type, null, true, 0, Collections.emptyList());
        }

        public NoUsageComponent(ComponentType type, String code) {
            super(type, code, true, 0, Collections.emptyList());
        }

        public NoUsageComponent(String type, String code) {
            super(ComponentType.getComponentTypeFromTypeName(type), code, true, 0, Collections.emptyList());
        }
    }

    public static class IrrelevantComponentUsage extends NoUsageComponent {

        public IrrelevantComponentUsage(ComponentType type, String code) {
            super(type, code);
        }
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    public static class EntandoCoreComponentReference {

        @JsonSerialize(using = EntandoCoreComponentTypeSerializer.class)
        @JsonDeserialize(using = EntandoCoreComponentTypeDeserializer.class)
        private ComponentType type;
        private String code;
        private Boolean online;

    }
}
