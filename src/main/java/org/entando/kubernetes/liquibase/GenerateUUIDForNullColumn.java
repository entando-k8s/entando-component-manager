package org.entando.kubernetes.liquibase;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;
import liquibase.change.custom.CustomTaskChange;
import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CustomChangeException;
import liquibase.exception.SetupException;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Setter
@Slf4j
public class GenerateUUIDForNullColumn implements CustomTaskChange {

    private String columnName;
    private String primaryKeyColumn = "id";

    @Override
    public void execute(Database database) throws CustomChangeException {
        JdbcConnection databaseConnection = (JdbcConnection) database.getConnection();
        try (Statement stmt = databaseConnection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,
                ResultSet.CONCUR_UPDATABLE)) {
            // select star doesn't work with oracle
            // https://stackoverflow.com/questions/18886864/invalid-operation-for-read-only-resultset-updatestring
            String query = String.format("SELECT %s,%s FROM installed_entando_bundles WHERE %s is null",
                    primaryKeyColumn, columnName, columnName);
            log.debug("to find record to update UUID execute query:'{}'", query);
            ResultSet uprs = stmt.executeQuery(query);
            while (uprs.next()) {
                String pk = uprs.getString(primaryKeyColumn);
                String generatedUuuid = UUID.randomUUID().toString();
                log.debug("find row update pk column:'{}' value:'{}' update column:'{}' generated UUID value:'{}'",
                        primaryKeyColumn, pk, columnName, generatedUuuid);
                uprs.updateString(columnName, generatedUuuid);
                uprs.updateRow();
            }
        } catch (Exception ex) {
            log.error("Error executing update uuid", ex);
            throw new CustomChangeException(ex);
        }
    }

    @Override
    public String getConfirmationMessage() {
        return String.format("Executed CustomTaskChange for class:'%s' on column:'%s' with pk column:'%s'",
                this.getClass(), columnName, primaryKeyColumn);
    }

    @Override
    public void setUp() throws SetupException {
        // do nothing
    }

    @Override
    public void setFileOpener(ResourceAccessor resourceAccessor) {
        // do nothing
    }

    @Override
    public ValidationErrors validate(Database database) {
        return null;
    }
}