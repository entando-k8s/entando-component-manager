package org.entando.kubernetes.model.bundle.descriptor;

import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageDescriptor implements Descriptor {

    private String code;
    private String parentCode;
    private Map<String, String> titles;
    private String pageModel;
    private String ownerGroup;
    private List<String> joinGroups;
    private boolean displayedInMenu;
    private boolean seo;
    private String charset;
    private String status;

}
