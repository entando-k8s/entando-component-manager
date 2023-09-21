package org.entando.kubernetes.service.update;

import java.io.IOException;
import java.sql.SQLException;
import javax.xml.parsers.ParserConfigurationException;
import liquibase.exception.LiquibaseException;
import org.entando.kubernetes.config.tenant.TenantConfigDTO;

public interface IUpdateDatabase {

    boolean isTenantDbUpdatePending(TenantConfigDTO tenantConfig) throws SQLException, LiquibaseException;

    void updateTenantDatabase(TenantConfigDTO tenantConfig) throws SQLException;

    void updateTenantDatabaseByDiff(TenantConfigDTO tenantConfig) throws SQLException, LiquibaseException,
            ParserConfigurationException, IOException;

    void generateDiff(TenantConfigDTO tenantConfig, String changelog) throws SQLException, LiquibaseException,
            ParserConfigurationException, IOException;

    void generateDiff(TenantConfigDTO reference, TenantConfigDTO target, String changelogFile) throws SQLException,
            LiquibaseException, IOException, ParserConfigurationException;

    void updateDatabase(TenantConfigDTO targetTenant, String changelog)  throws LiquibaseException, SQLException;
}
