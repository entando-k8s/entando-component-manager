package org.entando.kubernetes.client.hub.domain;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.jackson.Jacksonized;

import java.util.Set;

@Data
@NoArgsConstructor
public class OrganisationEntityDto {
    private Long id;

    private String name;

    private String description;

    private Set<BundleGroupEntityDto> bundleGroups;

    private Set<PortalUserEntityDto> portalUsers;

}
