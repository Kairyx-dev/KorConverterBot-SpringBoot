package org.specter.converter.adapter.persistence;

import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.specter.converter.adapter.persistence.configuration.PersistenceAutoConfiguration;

public abstract class AdapterTestBase {

    static final PostgreSQLContainer<?> PG =
            new PostgreSQLContainer<>("postgres:17-alpine").withReuse(true);

    static {
        PG.start();
    }

    @DynamicPropertySource
    static void dbProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", PG::getJdbcUrl);
        registry.add("spring.datasource.username", PG::getUsername);
        registry.add("spring.datasource.password", PG::getPassword);
    }

    @EnableAutoConfiguration
    @Import(PersistenceAutoConfiguration.class)
    public static class AdapterTestConfig {

        @Bean
        Flyway flyway(DataSource dataSource) {
            Flyway flyway = Flyway.configure()
                    .dataSource(dataSource)
                    .locations("classpath:db/migration")
                    .cleanDisabled(false)
                    .load();
            flyway.clean();
            flyway.migrate();
            return flyway;
        }
    }
}
