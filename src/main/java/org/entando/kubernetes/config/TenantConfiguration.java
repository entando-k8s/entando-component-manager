/*
 * Copyright 2023-Present Entando S.r.l. (http://www.entando.com) All rights reserved.
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
package org.entando.kubernetes.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.entando.kubernetes.config.tenant.TenantConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class TenantConfiguration {

    @Value("${ENTANDO_TENANTS:}")
    private String tenantsConfigAsString;
    
    @Autowired
    private ObjectMapper objectMapper;

    @Bean
    public Map<String, TenantConfig> tenantConfigs() {
        try {
            if (StringUtils.isNotBlank(this.tenantsConfigAsString)) {
                Map<String, TenantConfig> map = this.objectMapper.readValue(tenantsConfigAsString, new TypeReference<List<Map<String, String>>>() {
                })
                        .stream()
                        .map(TenantConfig::new)
                        .collect(Collectors.toMap(TenantConfig::getTenantCode, tc -> tc));
                log.debug("Readed tenant configuration - tenants {}", map.keySet());
                return map;
            } else {
                return new HashMap<>();
            }
        } catch (IOException exp) {
            throw new RuntimeException("Problem reading tenant configuration", exp);
        }
    }

}
