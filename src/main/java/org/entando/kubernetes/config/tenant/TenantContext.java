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

package org.entando.kubernetes.config.tenant;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.lang3.SerializationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TenantContext {

    private static Logger logger = LoggerFactory.getLogger(TenantContext.class);
    
    private static final String CURRENT_TENANT = "current_tenant";
    
    private TenantContext() {
        throw new IllegalStateException("Utility class");
    }
    
    public static String getCurrentTenant() {
        Serializable code = get(CURRENT_TENANT);
        if (code != null) {
            return code.toString();
        }
        return null;
    }

    public static void setCurrentTenant(String tenant) {
        set(CURRENT_TENANT, tenant);
    }
    
    private static final ThreadLocal<Map<String, Serializable>> threadLocalMap = new InheritableThreadLocal<>() {
        
        @Override
        protected Map<String, Serializable> childValue(Map<String, Serializable> parentValue) {
            if (parentValue == null) {
                return null;
            }
            Map<String, Serializable> map = new ConcurrentHashMap<>();

            for (Map.Entry<String, Serializable> entry : parentValue.entrySet()) {
                if (entry.getValue() == null) {
                    continue;
                }
                if (entry.getValue() instanceof String) {
                    map.put(entry.getKey(), entry.getValue());
                } else {
                    map.put(entry.getKey(), SerializationUtils.clone(entry.getValue()));
                }
            }
            return map;
        }
    };

    public static void clear() {
        Map<String, Serializable> map = getOrCreate();
        logger.debug("clear map with num of elements:'{}'", map.size());
        map.clear();
    }

    public static void destroy() {
        Map<String, Serializable> map = getOrCreate();
        logger.debug("destroy map with num of elements:'{}'", map.size());
        map.clear();
        threadLocalMap.remove();
    }

    private static Map<String, Serializable> getOrCreate() {
        Map<String, Serializable> map = threadLocalMap.get();
        if (null == map) {
            threadLocalMap.set(new HashMap<>());
            map = threadLocalMap.get();
        }
        return map;
    }

    public static void set(String key, Serializable value) {
        Map<String, Serializable> map = getOrCreate();
        logger.debug("set element in map with key:'{}'", key);
        map.put(key, value);
    }

    public static Serializable get(String key) {
        Map<String, Serializable> map = getOrCreate();
        logger.debug("get element from map with key:'{}'", key);
        return map.get(key);
    }

    public static void remove(String key) {
        Map<String, Serializable> map = threadLocalMap.get();
        if (null != map) {
            logger.debug("remove element from map with key:'{}'", key);
            map.remove(key);
        }
    }
    
}
