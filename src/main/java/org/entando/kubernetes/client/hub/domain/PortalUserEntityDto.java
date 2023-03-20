package org.entando.kubernetes.client.hub.domain;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.util.HashSet;
import java.util.Set;

@Data
public class PortalUserEntityDto {

    private Long id;

    private String username;

    private String email;

    private Set<OrganisationEntityDto> organisations = new HashSet<>();

}
