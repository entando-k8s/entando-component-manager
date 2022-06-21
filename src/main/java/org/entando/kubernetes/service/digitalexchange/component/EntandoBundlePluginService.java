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

import org.entando.kubernetes.model.job.PluginData;
import org.entando.kubernetes.model.web.request.PagedListRequest;
import org.entando.kubernetes.model.web.response.PagedMetadata;

public interface EntandoBundlePluginService {

    /**
     * This method retrieves the list of installed plugins based on the input bundle identifier.
     *
     * @param requestList the data to filter and sort the list of plugins
     * @param bundleId    the bundle identifier
     * @return the list of installed plugins, it can be empty
     */
    PagedMetadata<PluginData> getInstalledPluginsByBundleId(PagedListRequest requestList,
            String bundleId);

    /**
     * This method retrieves the list of installed plugins based on the input encoded URL.
     *
     * @param requestList the data to filter and sort the list of plugins
     * @param encodedUrl  a valid URL encoded with base64 algorithm used to generate the bundleId
     * @return the list of installed plugins, it can be empty
     */
    PagedMetadata<PluginData> getInstalledPluginsByEncodedUrl(PagedListRequest requestList,
            String encodedUrl);

    /**
     * This method retrieves the data of the installed plugin based on the input identifiers.
     *
     * @param bundleId   the bundle identifier
     * @param pluginName the plugin name identifier
     * @return the installed plugin data
     */
    PluginData getInstalledPlugin(String bundleId, String pluginName);

    /**
     * This method retrieves the data of the installed plugin based on the input identifiers.
     *
     * @param encodedUrl a valid URL encoded with base64 algorithm used to generate the bundleId
     * @param pluginName the plugin name identifier
     * @return the installed plugin data
     */
    PluginData getInstalledPluginByEncodedUrl(String encodedUrl, String pluginName);

}