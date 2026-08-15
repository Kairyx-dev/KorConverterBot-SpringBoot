package org.specter.converter.adapter.persistence.configuration;

import org.jooq.DSLContext;
import org.specter.converter.adapter.persistence.health.DatabaseHealthIndicator;
import org.specter.converter.adapter.persistence.port.IgnoreUserPersistenceAdapter;
import org.specter.converter.adapter.persistence.port.IgnoreUserQueryAdapter;
import org.specter.converter.adapter.persistence.port.MessageLogRecordAdapter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class PersistenceAutoConfiguration {

    @Bean
    public IgnoreUserPersistenceAdapter ignoreUserPersistenceAdapter(
            DSLContext dsl, ApplicationEventPublisher eventPublisher) {
        return new IgnoreUserPersistenceAdapter(dsl, eventPublisher);
    }

    @Bean
    public IgnoreUserQueryAdapter ignoreUserQueryAdapter(DSLContext dsl) {
        return new IgnoreUserQueryAdapter(dsl);
    }

    @Bean
    public MessageLogRecordAdapter messageLogRecordAdapter(DSLContext dsl) {
        return new MessageLogRecordAdapter(dsl);
    }

    @Bean
    public DatabaseHealthIndicator databaseHealthIndicator(DSLContext dsl) {
        return new DatabaseHealthIndicator(dsl);
    }
}
