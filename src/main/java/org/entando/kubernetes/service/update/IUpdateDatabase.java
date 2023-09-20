package org.entando.kubernetes.service.update;

import org.entando.kubernetes.config.tenant.TenantConfigDTO;

public interface IUpdateDatabase {

    boolean isTenantDbUpdatePending(TenantConfigDTO tenantConfig) throws Exception;

    void updateTenantDatabase(TenantConfigDTO tenantConfig) throws Exception;

    void updateTenantDatabaseByDiff(TenantConfigDTO tenantConfig)
            throws Exception;

    void generateDiff(TenantConfigDTO tenantConfig, String changelog)
            throws Exception;

    void generateDiff(TenantConfigDTO reference, TenantConfigDTO target, String changelogFile)
            throws Exception;

    void updateDatabase(TenantConfigDTO targetTenant, String changelog) throws Exception;
}
