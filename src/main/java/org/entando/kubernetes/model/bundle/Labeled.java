package org.entando.kubernetes.model.bundle;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.entando.kubernetes.model.job.EntandoBundleEntity;


public interface Labeled {

    Labels getLabels();

    void setLabels(Labels labels);

    /**
     * get pbc labels from the received EntandoBundleEntity if available, set them and return the current object.
     * @param bundleEntity the EntandoBundleEntity from which get the pbc names
     * @return the current object
     */
    default Labeled setPbcLabelsFrom(EntandoBundleEntity bundleEntity) {
        List<String> pbcList = getPbcLabelsFrom(bundleEntity);
        if (getLabels() == null) {
            setLabels(new Labels());
        }
        getLabels().setPbcNames(pbcList);
        return this;
    }

    /**
     * get pbc labels from the received EntandoBundleEntity if available and return them.
     * @param bundleEntity the EntandoBundleEntity from which get the pbc names
     * @return the current object
     */
    static List<String> getPbcLabelsFrom(EntandoBundleEntity bundleEntity) {
        return Optional.ofNullable(bundleEntity)
                .map(EntandoBundleEntity::getPbcList)
                .filter(ObjectUtils::isNotEmpty)
                .map(pbcs -> Arrays.asList(pbcs.split(",")))
                .orElse(null);
    }


    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    class Labels {
        private List<String> pbcNames;
    }

}
