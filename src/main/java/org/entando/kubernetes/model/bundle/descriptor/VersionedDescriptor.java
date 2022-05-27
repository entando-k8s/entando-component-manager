package org.entando.kubernetes.model.bundle.descriptor;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.ObjectUtils;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@Accessors(chain = true)
public abstract class VersionedDescriptor implements Descriptor {

    /**
     * identifies the descriptor version.
     */
    private String descriptorVersion;

    public boolean isVersion1() {
        return ObjectUtils.isEmpty(descriptorVersion)
                || descriptorVersion.equals(DescriptorVersion.V1);
    }
}
