package org.entando.kubernetes.model.bundle.descriptor;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@Accessors(chain = true)
@Slf4j
public abstract class VersionedDescriptor implements Descriptor {

    /**
     * identifies the descriptor version.
     */
    private String descriptorVersion;

    public boolean isVersion1() {
        return ObjectUtils.isEmpty(descriptorVersion)
                || descriptorVersion.equals(DescriptorVersion.V1.getVersion());
    }

    /**
     * check if the current descriptor version is equal or greater than the received one.
     *
     * @param version the version to use as max version
     * @return true if the current descriptor version is equal or greater than the received one
     */
    public boolean isVersionEqualOrGreaterThan(DescriptorVersion version) {
        try {
            int numDescriptorVersion = Integer.parseInt(descriptorVersion.replace("v", ""));
            int paramVersion = Integer.parseInt(version.getVersion().replace("v", ""));
            return numDescriptorVersion >= paramVersion;
        } catch (NumberFormatException e) {
            log.error(String.format("Error parsing descriptor version %s. Can't determine if it is major than %s",
                    this.descriptorVersion, version.getVersion()));
            return false;
        }
    }
}
