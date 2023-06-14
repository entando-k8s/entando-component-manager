package org.entando.kubernetes.model.bundle.usage;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.entando.kubernetes.model.entandocore.EntandoCoreComponentUsage;
import org.entando.kubernetes.model.entandocore.EntandoCoreComponentUsage.EntandoCoreComponentReference;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class ComponentUsage {

    private String type;
    private String code;
    private boolean exist;
    private int usage;
    private boolean hasExternal;
    private List<ComponentReference> references;


    public boolean getHasExternal() {
        return hasExternal;
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    @Builder
    public static class ComponentReference {

        private String componentType;
        private ComponentReferenceType referenceType;
        private String code;
        @JsonInclude(Include.NON_NULL)
        private Boolean online;


        /**
         * Utility method to copy the fields present in the EntandoCoreComponentReference within the ComponentReference
         * (used to contain and enrich the information present in the object returned by Entando Core).
         * ComponentReference specific fields (for which there is no correspondence in the object returned by Entando
         * Core) are not set as this is left to the business logic applied by the appropriate services.
         *
         * @param coreComponentReference portion of the response obtained from Entando Core concerning references for a
         *                               specific component
         * @return an object of type ComponentReference in which only the relative fields obtained from
         *     EntandoCoreComponentReference are set
         */
        public static ComponentReference fromEntandoCore(EntandoCoreComponentReference coreComponentReference) {
            return ComponentReference.builder().componentType(coreComponentReference.getType())
                    // the referenceType is not provided by the Entando Core but is computed by Component Manager, so
                    // the value is set only as reminder to make explicit this fact.
                    .referenceType(null)
                    .code(coreComponentReference.getCode())
                    .online(coreComponentReference.getOnline()).build();
        }

    }

    public enum ComponentReferenceType {
        EXTERNAL, INTERNAL
    }

    /**
     * Utility method to copy the fields present in the EntandoCoreComponentUsage within the ComponentUsage (used to
     * contain and enrich the information present in the object returned by Entando Core). ComponentUsage specific
     * fields (for which there is no correspondence in the object returned by Entando Core) are not set as this is left
     * to the business logic applied by the appropriate services.
     *
     * @param coreComponentUsage the response obtained from Entando Core concerning the information about a component
     * @return an object of type ComponentUsage in which only the relative fields obtained EntandoCoreComponentUsage are
     *     set
     */
    public static ComponentUsage fromEntandoCore(EntandoCoreComponentUsage coreComponentUsage) {
        return ComponentUsage.builder().type(coreComponentUsage.getType()).code(coreComponentUsage.getCode())
                .exist(coreComponentUsage.isExist()).usage(coreComponentUsage.getUsage())
                // the hasExternal field is not provided by the Entando Core but is computed by Component Manager, so
                // the value is set only as reminder to make explicit this fact.
                .hasExternal(false)
                .references(
                        Optional.ofNullable(coreComponentUsage.getReferences()).orElse(new ArrayList<>()).stream()
                                .filter(Objects::nonNull).map(ComponentReference::fromEntandoCore)
                                .collect(Collectors.toList())).build();
    }
}
