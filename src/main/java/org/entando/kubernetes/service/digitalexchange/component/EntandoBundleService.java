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

import java.util.List;
import java.util.Optional;
import org.entando.kubernetes.controller.digitalexchange.component.EntandoBundle;
import org.entando.kubernetes.model.job.EntandoBundleComponentJob;
import org.entando.kubernetes.model.web.request.PagedListRequest;
import org.entando.kubernetes.model.web.response.PagedMetadata;

public interface EntandoBundleService {

    PagedMetadata<EntandoBundle> getComponents();

    PagedMetadata<EntandoBundle> getComponents(PagedListRequest request);

    Optional<EntandoBundle> getInstalledComponent(String id);

    List<EntandoBundleComponentJob> getBundleInstalledComponents(String id);
}
