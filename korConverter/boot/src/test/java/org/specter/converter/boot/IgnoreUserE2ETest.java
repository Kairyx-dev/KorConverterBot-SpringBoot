package org.specter.converter.boot;

import static org.assertj.core.api.Assertions.assertThat;

import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.specter.converter.application.dto.command.AddIgnoreUserCommand;
import org.specter.converter.application.dto.command.RemoveIgnoreUserCommand;
import org.specter.converter.application.dto.query.CheckIgnoreUserQuery;
import org.specter.converter.application.port.input.AddIgnoreUserUseCase;
import org.specter.converter.application.port.input.CheckIgnoreUserUseCase;
import org.specter.converter.application.port.input.RemoveIgnoreUserUseCase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

@SpringBootTest(
    classes = IgnoreUserE2ETest.E2ETestConfig.class,
    properties = {
        "spring.autoconfigure.exclude="
            + "org.specter.converter.adapter.bot.configuration.BotAutoConfiguration"
    })
class IgnoreUserE2ETest {

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
    @Import({
        org.specter.converter.adapter.persistence.configuration.PersistenceAutoConfiguration.class,
        org.specter.converter.configuration.ConverterBeanAutoConfiguration.class
    })
    static class E2ETestConfig {

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

    @Autowired
    AddIgnoreUserUseCase addUseCase;

    @Autowired
    CheckIgnoreUserUseCase checkUseCase;

    @Autowired
    RemoveIgnoreUserUseCase removeUseCase;

    @Test
    void full_lifecycle() {
        // Add
        var result = addUseCase.execute(new AddIgnoreUserCommand(111L, 222L, "e2eUser"));
        assertThat(result.name()).isEqualTo("e2eUser");
        assertThat(result.userId()).isEqualTo(111L);
        assertThat(result.channelId()).isEqualTo(222L);

        // Check exists
        assertThat(checkUseCase.execute(new CheckIgnoreUserQuery(111L, 222L))).isTrue();

        // Remove
        removeUseCase.execute(new RemoveIgnoreUserCommand(111L, 222L));

        // Check removed
        assertThat(checkUseCase.execute(new CheckIgnoreUserQuery(111L, 222L))).isFalse();
    }
}
