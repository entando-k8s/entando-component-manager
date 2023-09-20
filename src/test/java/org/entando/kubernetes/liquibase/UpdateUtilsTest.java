package org.entando.kubernetes.liquibase;

import static org.entando.kubernetes.service.update.UpdateUtils.getSchemaFromJdbc;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

public class UpdateUtilsTest {

    @Test
    void testJdbcSchemaPostgres() {
        String jdbcUrl = "jdbc:postgresql://default-postgresql-dbms-in-namespace-service.test-mt-pj.svc.cluster.local:"
                + "5432/default_postgresql_dbms_in_namespace_db?param1=value1&currentSchema=tenant1_cmschema";

        String schema = getSchemaFromJdbc(jdbcUrl);
        assertNotNull(schema);
        assertThat(schema, is("tenant1_cmschema"));

        jdbcUrl = "String jdbcUrl = \"jdbc:postgresql://default-postgresql-dbms-in-namespace-service.test-mt-pj."
                + "svc.cluster.local:5432/default_postgresql_dbms_in_namespace_db?param1=value1";
        schema = getSchemaFromJdbc(jdbcUrl);
        assertNull(schema);
    }

    @Test
    void testNullArgs() {
        Exception exception = assertThrows(NullPointerException.class, () -> {
            getSchemaFromJdbc(null);
        });
    }

}
