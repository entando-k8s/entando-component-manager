package org.entando.kubernetes.liquibase;


import static org.entando.kubernetes.liquibase.helper.DbMigrationUtils.generateSecureRandomHash;
import static org.entando.kubernetes.liquibase.helper.DbMigrationUtils.getSchemaFromJdbc;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles({"test"})
class DbMigrationUtilsTest {

    @Test
    void testExtractSchemaFromJDBCurl() {
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
    void testNullArgsThrowException() {
        assertThrows(NullPointerException.class, () -> getSchemaFromJdbc(null));
    }

    @Test
    void testHashGenerationWithDifferentLength() {
        String hash = generateSecureRandomHash(6);
        validateHash(hash, 6);

        hash = generateSecureRandomHash(1);
        validateHash(hash, 1);

        hash = generateSecureRandomHash(0);
        validateHash(hash, 0);
    }

    private void validateHash(String hash, int expectedLength) {
        assertNotNull(hash);
        MatcherAssert.assertThat(hash, Matchers.hasLength(expectedLength));
    }

}
