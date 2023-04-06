package org.entando.kubernetes.client.hub.domain;

import java.util.Set;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class OrganisationEntityDto {

    private Long id;

    private String name;

    private String description;

    private Set<BundleGroupEntityDto> bundleGroups;

    private Set<PortalUserEntityDto> portalUsers;

}
