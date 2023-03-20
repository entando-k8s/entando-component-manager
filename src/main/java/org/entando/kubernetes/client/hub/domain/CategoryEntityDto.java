package org.entando.kubernetes.client.hub.domain;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.util.HashSet;
import java.util.Set;

@Data
@Builder
public class CategoryEntityDto {

    private Long id;

    private String name;

    private String description;

    private Set<BundleGroupEntityDto> bundleGroups = new HashSet<>();

}
