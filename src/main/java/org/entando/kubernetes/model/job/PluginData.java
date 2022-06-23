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

import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class PluginData {

    private UUID id;
    private String bundleId;
    private String pluginId;
    private String pluginName;
    private String pluginCode;
    private String ingressPath;
    private String[] roles;

    public static PluginData fromEntity(PluginDataEntity entity) {
        return PluginData.builder()
                .id(entity.getId())
                .bundleId(entity.getBundleId())
                .pluginId(entity.getPluginId())
                .pluginName(entity.getPluginName())
                .pluginCode(entity.getPluginCode())
                .ingressPath(entity.getEndpoint())
                .roles(manageRoles(entity.getRoles()))
                .build();
    }

    private static String[] manageRoles(Set<String> roles) {
        if (roles == null) {
            return new String[0];
        } else {
            return roles.stream().toArray(String[]::new);
        }
    }
}
