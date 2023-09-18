package org.entando.kubernetes.service.update;

import java.io.IOException;
import java.sql.SQLException;
import javax.xml.parsers.ParserConfigurationException;
import liquibase.exception.LiquibaseException;
import org.entando.kubernetes.config.tenant.TenantConfigDTO;

public interface IUpdateDatabase {

    boolean isTenantDbUpdatePending(TenantConfigDTO tenantConfig) throws IOException, LiquibaseException;

    void updateTenantDatabase(TenantConfigDTO tenantConfig) throws IOException, LiquibaseException;

    void generateDiff(TenantConfigDTO tenantConfig, String changelog)
            throws SQLException, LiquibaseException, ParserConfigurationException, IOException;

    void generateDiff(TenantConfigDTO reference, TenantConfigDTO target, String changelogFile)
            throws LiquibaseException, ParserConfigurationException, IOException;

    void updateDatabase(TenantConfigDTO targetTenant, String changelog) throws LiquibaseException;
}
