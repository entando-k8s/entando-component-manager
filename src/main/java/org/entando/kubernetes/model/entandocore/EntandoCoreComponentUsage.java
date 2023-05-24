package org.entando.kubernetes.model.entandocore;

import java.util.Collections;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.entando.kubernetes.model.bundle.ComponentType;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class EntandoCoreComponentUsage {

    private String type;
    private String code;
    private boolean exist;
    private int usage;
    private List<ComponentReference> references;

    public static class NoUsageComponent extends EntandoCoreComponentUsage {

        public NoUsageComponent(ComponentType type) {
            super(type.getTypeName(), null, true, 0, Collections.emptyList());
        }

        public NoUsageComponent(ComponentType type, String code) {
            super(type.getTypeName(), code, true, 0, Collections.emptyList());
        }

        public NoUsageComponent(String type, String code) {
            super(type, code, true, 0, Collections.emptyList());
        }
    }

    public static class IrrelevantComponentUsage extends NoUsageComponent {

        public IrrelevantComponentUsage(String code) {
            super("irrelevant", code);
        }
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    public static class ComponentReference {

        private String componentType;
        private ComponentReferenceType referenceType;
        private String code;

    }

    public enum ComponentReferenceType {
        EXTERNAL, INTERNAL;
    }

}
