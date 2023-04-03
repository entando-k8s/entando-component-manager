package org.entando.kubernetes.client.hub.domain;

import java.util.Set;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

/**
 * This entity class is for BUNDLE table.
 */
@Data
@Builder
@ToString
@Jacksonized
public class BundleEntityDto {

    private Long id;
    private String name;
    private String description;
    private String gitRepoAddress;
    private String gitSrcRepoAddress;
    private String dependencies;

    private HubDescriptorVersion descriptorVersion = HubDescriptorVersion.V1;

    private Set<BundleGroupVersionEntityDto> bundleGroupVersions;

}
