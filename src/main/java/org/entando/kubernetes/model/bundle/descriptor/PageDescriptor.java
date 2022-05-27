package org.entando.kubernetes.model.bundle.descriptor;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.entando.kubernetes.model.bundle.descriptor.widget.WidgetConfigurationDescriptor;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PageDescriptor implements Descriptor {

    private String code;
    private String parentCode;
    private String ownerGroup;
    private String pageModel;
    private Map<String, String> titles;
    private List<String> joinGroups;
    private boolean displayedInMenu;
    private boolean seo;
    private String charset;
    private String status;
    private List<WidgetConfigurationDescriptor> widgets;

    @Override
    public ComponentKey getComponentKey() {
        return new ComponentKey(code);
    }
}
