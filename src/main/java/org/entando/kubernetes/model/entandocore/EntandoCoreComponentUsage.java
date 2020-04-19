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

    public static class IrrelevantEntandoCoreComponentUsage extends EntandoCoreComponentUsage{

        public IrrelevantEntandoCoreComponentUsage(String code) {
            super("irrelevant", code, 0);
        }
    }
}
