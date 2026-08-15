package org.specter.converter.adapter.persistence.port;

import static org.assertj.core.api.Assertions.assertThat;
import static org.specter.converter.adapter.persistence.generated.Tables.IGNORE_USER;

import java.time.Instant;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.specter.converter.adapter.persistence.AdapterTestBase;
import org.specter.converter.domain.model.ChannelId;
import org.specter.converter.domain.model.IgnoreUser;
import org.specter.converter.domain.model.UserId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = AdapterTestBase.AdapterTestConfig.class)
class IgnoreUserQueryAdapterTest extends AdapterTestBase {

    @Autowired
    DSLContext dsl;

    @Autowired
    IgnoreUserQueryAdapter queryAdapter;

    @Autowired
    IgnoreUserPersistenceAdapter persistenceAdapter;

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @BeforeEach
    void cleanup() {
        dsl.deleteFrom(IGNORE_USER).execute();
    }

    @Test
    void existsByUserIdAndChannelId_returns_true_when_exists() {
        var user = IgnoreUser.create(new UserId(100L), new ChannelId(200L), "tester", NOW);
        persistenceAdapter.save(user);

        assertThat(queryAdapter.existsByUserIdAndChannelId(100L, 200L)).isTrue();
    }

    @Test
    void existsByUserIdAndChannelId_returns_false_when_not_exists() {
        assertThat(queryAdapter.existsByUserIdAndChannelId(999L, 999L)).isFalse();
    }

    @Test
    void findAllByChannelId_returns_matching_users() {
        persistenceAdapter.save(IgnoreUser.create(new UserId(1L), new ChannelId(300L), "alice", NOW));
        persistenceAdapter.save(IgnoreUser.create(new UserId(2L), new ChannelId(300L), "bob", NOW));
        persistenceAdapter.save(IgnoreUser.create(new UserId(3L), new ChannelId(999L), "other", NOW));

        var results = queryAdapter.findAllByChannelId(300L);
        assertThat(results).hasSize(2);
        assertThat(results).extracting("name").containsExactlyInAnyOrder("alice", "bob");
    }

    @Test
    void findAllByChannelId_returns_empty_when_no_match() {
        assertThat(queryAdapter.findAllByChannelId(888L)).isEmpty();
    }
}
