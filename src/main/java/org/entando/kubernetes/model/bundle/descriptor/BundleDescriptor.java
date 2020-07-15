package org.entando.kubernetes.model.bundle.descriptor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BundleDescriptor implements Descriptor {

    private String code;
    private String description;
    private String title;
    private BundleAuthorDescriptor author;
    private String organization;
    private String thumbnail;

    private ComponentSpecDescriptor components;

}
