package org.specter.converter.adapter.persistence.health;

import java.util.Objects;
import org.jooq.DSLContext;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;

/**
 * Reports database connectivity status via Actuator health endpoint.
 *
 * <p>Executes a minimal {@code SELECT 1} query via jOOQ. Returns {@code UP} on success, {@code
 * DOWN} with the exception on failure. Registered as a bean in {@link
 * org.specter.converter.adapter.persistence.configuration.PersistenceAutoConfiguration}.
 *
 * <p>Placed in adapter-persistence per purist-ddd-playbook Part 10 §10.1: observability code
 * (HealthIndicator) belongs in Configuration or Adapter, and DB connectivity is squarely a
 * persistence adapter concern. DSLContext is already available on this module's classpath.
 */
public class DatabaseHealthIndicator implements HealthIndicator {

    private final DSLContext dsl;

    public DatabaseHealthIndicator(DSLContext dsl) {
        this.dsl = Objects.requireNonNull(dsl, "dsl");
    }

    @Override
    public Health health() {
        try {
            dsl.selectOne().fetch();
            return Health.up().build();
        } catch (Exception e) {
            return Health.down(e).build();
        }
    }
}
