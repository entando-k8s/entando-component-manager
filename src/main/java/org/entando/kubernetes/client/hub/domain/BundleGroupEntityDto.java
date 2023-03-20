package org.entando.kubernetes.client.hub.domain;

import lombok.*;
import lombok.extern.jackson.Jacksonized;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * This entity class is for BUNDLE_GROUP table 
 *
 */
@Data
@ToString
public class BundleGroupEntityDto {

    private Long id;
    private String name;

    private Long catalogId;

    private Boolean publicCatalog;


    private OrganisationEntityDto organisation;


    private Set<CategoryEntityDto> categories = new HashSet<>();
    
    @OneToMany(mappedBy = "bundleGroup", fetch = FetchType.LAZY, cascade = {CascadeType.MERGE})
    private Set<BundleGroupVersionEntityDto> version = new HashSet<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BundleGroupEntityDto that = (BundleGroupEntityDto) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

