package org.entando.kubernetes.client.hub.domain;

import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.util.Set;

/**
 * This entity class is for BUNDLE table
 *
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
    
    private DescriptorVersion descriptorVersion = DescriptorVersion.V1;
    
	private Set<BundleGroupVersionEntityDto> bundleGroupVersions;

}
