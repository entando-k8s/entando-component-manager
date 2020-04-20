package org.entando.kubernetes.model.entandocore;

import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.entando.kubernetes.model.digitalexchange.ComponentType;
import org.springframework.cglib.core.HashCodeCustomizer;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class EntandoCoreComponentUsage {

    private String type;
    private String code;
    private int usage;

    @Override
    public int hashCode() {
        return Objects.hash(type, code);
    }

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
