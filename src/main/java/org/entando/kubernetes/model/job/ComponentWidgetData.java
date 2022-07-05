/*
 * Copyright 2019-Present Entando Inc. (http://www.entando.com) All rights reserved.
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

package org.entando.kubernetes.model.job;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.entando.kubernetes.model.bundle.descriptor.widget.WidgetDescriptor.WidgetExt;
import org.entando.kubernetes.service.digitalexchange.templating.WidgetTemplateGeneratorService.FtlSystemParams;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class ComponentWidgetData {

    // db fields
    private String id;
    private String bundleId;
    private String widgetCode;
    private String widgetName;
    private String widgetType;
    private String bundleGroup;

    // json fields and metadata
    private String customElement;
    private String[] assets;
    private WidgetExt descriptorExt;
    private FtlSystemParams systemParams;

}
