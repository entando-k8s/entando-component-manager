package org.entando.kubernetes.model.bundle.descriptor;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FragmentDescriptor implements Descriptor {

    private String code;
    private String guiCode;
    private String guiCodePath;

}
