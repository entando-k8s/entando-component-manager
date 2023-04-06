package org.entando.kubernetes.client.hub.domain;

import java.util.HashSet;
import java.util.Set;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CategoryEntityDto {

    private Long id;

    private String name;

    private String description;

    private Set<BundleGroupEntityDto> bundleGroups = new HashSet<>();

}
