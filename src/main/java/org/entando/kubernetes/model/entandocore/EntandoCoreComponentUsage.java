package org.entando.kubernetes.model.entandocore;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.entando.kubernetes.model.bundle.ComponentType;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class EntandoCoreComponentUsage {

    private String type;
    private String code;
    private int usage;

    public static class NoUsageComponent extends EntandoCoreComponentUsage {

        public NoUsageComponent(ComponentType type) {
            super(type.getTypeName(), null, 0);
        }

        public NoUsageComponent(ComponentType type, String code) {
            super(type.getTypeName(), code, 0);
        }

        public NoUsageComponent(String type, String code) {
            super(type, code, 0);
        }
    }

    public static class IrrelevantComponentUsage extends NoUsageComponent {

        public IrrelevantComponentUsage(String code) {
            super("irrelevant", code);
        }
    }

}
