package org.entando.kubernetes.client.hub.domain;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class BundleGroupVersionEntityDto {

    private Long id;
    private String description;
    private String documentationUrl;
    private String version;
    private String descriptionImage;
    private BundleGroupVersionStatus status = BundleGroupVersionStatus.NOT_PUBLISHED;
    private Boolean displayContactUrl;
    private String contactUrl;
    private BundleGroupEntityDto bundleGroup;
    private Set<BundleEntityDto> bundles = new HashSet<>();
    private LocalDateTime lastUpdated;
}
