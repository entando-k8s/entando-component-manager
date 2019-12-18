package org.entando.kubernetes.model.entandocore;

import lombok.Data;
import org.entando.kubernetes.model.bundle.descriptor.FragmentDescriptor;

@Data
public class EntandoCoreFragment {

    private String code;
    private String guiCode;

    public EntandoCoreFragment(FragmentDescriptor descriptor) {
        this.code = descriptor.getCode();
        this.guiCode = descriptor.getGuiCode();
    }

}
