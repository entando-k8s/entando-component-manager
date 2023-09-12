package org.entando.kubernetes.service.update;

import java.io.IOException;
import liquibase.exception.LiquibaseException;
import org.entando.kubernetes.config.tenant.TenantConfigDTO;

public interface IUpdateDatabase {

    boolean isTenantDbUpdatePending(TenantConfigDTO tenantConfig) throws IOException, LiquibaseException;

    void updateTenantDatabase(TenantConfigDTO tenantConfig) throws IOException, LiquibaseException;
}
