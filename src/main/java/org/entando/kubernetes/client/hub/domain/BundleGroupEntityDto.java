package org.entando.kubernetes.client.hub.domain;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class BundleGroupEntityDto {

    private Long id;
    private String name;
    private Long catalogId;
    private Boolean publicCatalog;
    private OrganisationEntityDto organisation;
    private Set<CategoryEntityDto> categories = new HashSet<>();
    private Set<BundleGroupVersionEntityDto> version = new HashSet<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BundleGroupEntityDto that = (BundleGroupEntityDto) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
