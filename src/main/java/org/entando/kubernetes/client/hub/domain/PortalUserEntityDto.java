package org.entando.kubernetes.client.hub.domain;

import java.util.HashSet;
import java.util.Set;
import lombok.Data;

@Data
public class PortalUserEntityDto {

    private Long id;

    private String username;

    private String email;

    private Set<OrganisationEntityDto> organisations = new HashSet<>();

}
