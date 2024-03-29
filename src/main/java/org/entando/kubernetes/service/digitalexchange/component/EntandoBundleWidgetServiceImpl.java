/*
 * Copyright 2018-Present Entando Inc. (http://www.entando.com) All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package org.entando.kubernetes.service.digitalexchange.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.entando.kubernetes.model.bundle.BundleProperty;
import org.entando.kubernetes.model.bundle.BundleType;
import org.entando.kubernetes.model.bundle.ComponentType;
import org.entando.kubernetes.model.bundle.descriptor.DescriptorVersion;
import org.entando.kubernetes.model.bundle.descriptor.widget.WidgetDescriptor;
import org.entando.kubernetes.model.job.ComponentDataEntity;
import org.entando.kubernetes.model.job.ComponentWidgetData;
import org.entando.kubernetes.model.job.EntandoBundleEntity;
import org.entando.kubernetes.model.web.request.PagedListRequest;
import org.entando.kubernetes.model.web.response.PagedMetadata;
import org.entando.kubernetes.repository.ComponentDataRepository;
import org.entando.kubernetes.repository.InstalledEntandoBundleRepository;
import org.entando.kubernetes.service.digitalexchange.BundleUtilities;
import org.entando.kubernetes.service.digitalexchange.JSONUtilities;
import org.entando.kubernetes.validator.ValidationFunctions;
import org.springframework.stereotype.Service;
import org.zalando.problem.Problem;
import org.zalando.problem.Status;

@Service
@Slf4j
@AllArgsConstructor
public class EntandoBundleWidgetServiceImpl implements EntandoBundleWidgetService {

    private final ComponentDataRepository componentDataRepository;
    private static final ObjectMapper jsonMapper = new ObjectMapper();
    private final InstalledEntandoBundleRepository installedComponentRepo;

    @Override
    public PagedMetadata<ComponentWidgetData> listWidgets(PagedListRequest request) {
        // fetch from db
        List<ComponentDataEntity> allWidgetEntities = componentDataRepository.findAll().stream()
                .filter(c -> ComponentType.WIDGET.equals(c.getComponentType()))
                .collect(Collectors.toList());

        // collect all distinct bundleId
        final List<String> bundleIdSet = new ArrayList<>(allWidgetEntities.stream()
                .map(ComponentDataEntity::getBundleId)
                .collect(Collectors.toSet()));

        // collect map bundleId -> bundleCode
        Map<String, String> bundleIdBundleCodeMap = new HashMap<>();
        IntStream.range(0, bundleIdSet.size())
                .forEach(i ->
                    installedComponentRepo.findByBundleId(bundleIdSet.get(i))
                            .map(b -> bundleIdBundleCodeMap.putIfAbsent(bundleIdSet.get(i), b.getBundleCode())));

        // convert to dto
        List<ComponentWidgetData> allWidgets = allWidgetEntities.stream()
                .map(this::convertToComponentWidgetData)
                .map(this::populatePbcList)
                .map(this::composeBaseAssetsPath)
                .map(w -> w.setBundleCode(bundleIdBundleCodeMap.getOrDefault(w.getBundleId(), null)))
                .collect(Collectors.toList());

        List<ComponentWidgetData> localFilteredList = new ComponentWidgetDataListProcessor(request, allWidgets)
                .filterAndSort().toList();
        List<ComponentWidgetData> sublist = request.getSublist(localFilteredList);

        return new PagedMetadata<>(request, sublist, localFilteredList.size());

    }

    private ComponentWidgetData convertToComponentWidgetData(ComponentDataEntity entity) {
        WidgetDescriptor widgetDescriptor = null;
        try {
            widgetDescriptor = (WidgetDescriptor) JSONUtilities
                    .deserializeDescriptorOrThrow(entity.getComponentDescriptor(), WidgetDescriptor.class);
        } catch (Exception ex) {
            log.error("error marshalling widgetDescriptor from db for ComponentDataEntity with id:'{}'",
                    entity.getId().toString(), ex);
            throw Problem.valueOf(Status.INTERNAL_SERVER_ERROR,
                    String.format(
                            "error marshalling widgetDescriptor from db for ComponentDataEntity with id:'%s' error:'%s'",
                            entity.getId().toString(), ex.getMessage()));
        }

        String descriptorExt = widgetDescriptor.getExt() != null ? widgetDescriptor.getExt().getAppBuilder() : null;

        return ComponentWidgetData.builder()
                .id(entity.getId().toString())
                .bundleId(entity.getBundleId())
                .bundleCode(widgetDescriptor.getDescriptorMetadata().getBundleCode())
                .widgetCode(entity.getComponentCode())
                .widgetName(entity.getComponentName())
                .widgetType(entity.getComponentSubType())
                .permGroup(entity.getComponentGroup())
                .customElement(widgetDescriptor.getCustomElement())
                .assets(widgetDescriptor.getDescriptorMetadata().getAssets())
                .descriptorExt(descriptorExt)
                .systemParams(widgetDescriptor.getDescriptorMetadata().getSystemParams())
                .desriptorVersion(determineWidgetDescriptorVersion(widgetDescriptor.getDescriptorVersion(),
                        entity.getComponentSubType()))
                .build();

    }

    private ComponentWidgetData populatePbcList(ComponentWidgetData componentWidgetData) {
        Optional<EntandoBundleEntity> findByBundleId = installedComponentRepo
                .findByBundleId(componentWidgetData.getBundleId());
        return (findByBundleId.isPresent())
                ? (ComponentWidgetData) componentWidgetData.setPbcLabelsFrom(findByBundleId.get())
                : componentWidgetData;
    }

    private ComponentWidgetData composeBaseAssetsPath(ComponentWidgetData componentWidgetData) {

        // in some cases the descriptor saved in the db could be empty
        if (StringUtils.isBlank(componentWidgetData.getBundleCode())) {
            log.warn("Skipping widget " + componentWidgetData.getWidgetName() + " due to empty saved descriptor");
        } else {
            String widgetName = determineWidgetName(componentWidgetData);
            String path = BundleUtilities.buildFullBundleResourcePath(
                    BundleType.STANDARD_BUNDLE,
                    componentWidgetData.getDesriptorVersion(),
                    BundleProperty.WIDGET_FOLDER_PATH,
                    Paths.get(BundleProperty.WIDGET_FOLDER_PATH.getValue(), widgetName).toString(),
                    componentWidgetData.getBundleCode());

            componentWidgetData.setAssetsBasePath(path);
        }

        return componentWidgetData;
    }

    private String determineWidgetName(ComponentWidgetData componentWidgetData) {
        if (StringUtils.isNotBlank(componentWidgetData.getWidgetName())) {
            return componentWidgetData.getWidgetName();
        }

        final String widgetCode = componentWidgetData.getWidgetCode();
        if (ValidationFunctions.isEntityCodeValid(widgetCode)) {
            return BundleUtilities.extractNameFromEntityCode(widgetCode);
        }

        return widgetCode;
    }

    /**
     * this method address an old bug that wasn't writing the widget descriptor version inside the db. this method
     * provides a walkarount to determine the widget descriptor version
     *
     * @param descriptorVersion the descriptor version
     * @param widgetSubType     the widget sub type
     * @return the determined widget descriptor version
     */
    private String determineWidgetDescriptorVersion(String descriptorVersion, String widgetSubType) {
        if (StringUtils.isNotBlank(descriptorVersion)) {
            return descriptorVersion;
        }

        if (WidgetDescriptor.TYPE_WIDGET_STANDARD.equals(widgetSubType)) {
            return DescriptorVersion.V1.getVersion();
        }

        return DescriptorVersion.V5.getVersion();
    }
}
