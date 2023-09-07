package org.entando.kubernetes.service.update;

import org.entando.kubernetes.config.tenant.TenantConfigDTO;

public interface IUpdateDatabase {

    void updateTenantDatabase(TenantConfigDTO tenantConfig, String masterFilePath);
}
