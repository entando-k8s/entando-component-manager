package org.entando.kubernetes.model.bundle.descriptor;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FragmentDescriptor implements Descriptor {

    private String code;
    private String guiCode;
    private String guiCodePath;


}
