package org.entando.kubernetes.model.bundle.descriptor.content;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.entando.kubernetes.model.bundle.descriptor.ComponentKey;
import org.entando.kubernetes.model.bundle.descriptor.Descriptor;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentDescriptor implements Descriptor {

    private String id;
    private String typeCode;
    private String description;
    private String[] groups;
    private String mainGroup;
    private String status;
    private String viewPage;
    private String listModel;
    private String defaultModel;
    private ContentAttribute[] attributes;
    private List<String> categories;

    @Override
    public ComponentKey getComponentKey() {
        return new ComponentKey(id);
    }


    @Getter
    public enum ContentStatus {

        PUBLIC;
    }
}
