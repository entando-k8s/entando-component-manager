package org.entando.kubernetes.service;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.compress.utils.Sets;
import org.apache.commons.lang3.StringUtils;
import org.entando.kubernetes.controller.digitalexchange.component.model.EntandoBundleDTO;
import org.entando.kubernetes.model.bundle.BundleVersion;
import org.entando.kubernetes.model.bundle.EntandoBundle;
import org.entando.kubernetes.model.bundle.EntandoComponentBundle;
import org.entando.kubernetes.model.digitalexchange.ComponentType;
import org.entando.kubernetes.model.digitalexchange.EntandoBundleEntity;

public class ModelConverter {

    public static EntandoBundle fromEntity(EntandoBundleEntity entity) {
            return EntandoBundle.builder()
                    .id(entity.getId())
                    .ecrId(entity.getEcrId())
                    .code(entity.getCode())
                    .title(entity.getTitle())
                    .description(entity.getDescription())
                    .thumbnail(entity.getThumbnail())
                    .organization(entity.getOrganization())
                    .componentTypes(entity.getType())
                    .lastJob(entity.getLastJob())
                    .installedJob(entity.getInstalledJob())
                    .versions(new ArrayList<>())
                    .build();
    }

    public static EntandoBundle fromECR(EntandoComponentBundle ecb) {
        Set<String> bundleComponentTypes = Sets.newHashSet("bundle");
        if (ecb.getMetadata().getLabels() != null) {
            ecb.getMetadata().getLabels()
                    .keySet().stream()
                    .filter(ComponentType::isValidType)
                    .forEach(bundleComponentTypes::add);
        }

        return EntandoBundle.builder()
                .id(null)
                .ecrId(ecb.getMetadata().getName())
                .code(ecb.getSpec().getCode())
                .title(StringUtils.defaultIfBlank(ecb.getSpec().getTitle(), ecb.getSpec().getCode()))
                .description(ecb.getSpec().getDescription())
                .thumbnail(ecb.getSpec().getThumbnail())
                .organization(ecb.getSpec().getOrganization())
                .componentTypes(bundleComponentTypes)
                .versions(ecb.getSpec().getVersions().stream().map(v ->
                        BundleVersion.builder()
                                .version(v.getVersion())
                                .timestamp(ZonedDateTime.parse(v.getTimestamp()))
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }


}
