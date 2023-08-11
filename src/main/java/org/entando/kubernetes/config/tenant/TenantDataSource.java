package org.entando.kubernetes.config.tenant;

import org.entando.kubernetes.config.tenant.thread.TenantContextHolder;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

public class TenantDataSource extends AbstractRoutingDataSource {

    @Override
    protected Object determineCurrentLookupKey() {
        return TenantContextHolder.getCurrentTenantCode();
    }
}
