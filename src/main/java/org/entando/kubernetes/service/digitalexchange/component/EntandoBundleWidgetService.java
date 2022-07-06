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

import org.entando.kubernetes.model.job.ComponentWidgetData;
import org.entando.kubernetes.model.web.request.PagedListRequest;
import org.entando.kubernetes.model.web.response.PagedMetadata;

public interface EntandoBundleWidgetService {


    /**
     * This method retrieves the list of widgets for all installed bundles.
     *
     * @param request the data to filter and sort the list of widgets
     * @return the list of widgets, it can be empty
     */
    PagedMetadata<ComponentWidgetData> listWidgets(PagedListRequest request);

}