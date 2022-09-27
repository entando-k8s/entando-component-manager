package org.entando.kubernetes.config;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import javax.sql.DataSource;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.integration.spring.SpringLiquibase;
import liquibase.lockservice.DatabaseChangeLogLock;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

@Component
public class LiquibaseBeanPostProcessor implements BeanPostProcessor {

    @Value("${spring.liquibase.lock.fallback.minutes:10}")
    private int lockFallbackMinutes;

    private final DataSource dataSource;

    @Autowired
    public LiquibaseBeanPostProcessor(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        if (bean instanceof SpringLiquibase) {
            String changeLog = ((SpringLiquibase) bean).getChangeLog();
            Instant releaseLockLimit = Instant.now().minus(lockFallbackMinutes, ChronoUnit.MINUTES);
            try (Connection connection = dataSource.getConnection()) {
                Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
                try (Liquibase liquibase = new Liquibase(changeLog, new ClassLoaderResourceAccessor(), database)) {
                    for (DatabaseChangeLogLock lock : liquibase.listLocks()) {
                        if (lock.getLockGranted().toInstant().isBefore(releaseLockLimit)) {
                            liquibase.forceReleaseLocks();
                            break;
                        }
                    }
                }
            } catch (SQLException | LiquibaseException ex) {
                throw new RuntimeException(ex);
            }
        }
        return bean;
    }
}
