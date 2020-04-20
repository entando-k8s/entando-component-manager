package org.entando.kubernetes.model.entandocore;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class EntandoCoreComponentUsage {

    private String type;
    private String code;
    private int usage;

    @Override
    public int hashCode() {
        return 31 + type.hashCode() + code.hashCode();
    }

    public static class IrrelevantEntandoCoreComponentUsage extends EntandoCoreComponentUsage {

        public IrrelevantEntandoCoreComponentUsage(String code) {
            super("irrelevant", code, 0);
        }
    }
}
