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
class IgnoreUserPersistenceAdapterTest extends AdapterTestBase {

  @Autowired DSLContext dsl;

  @Autowired IgnoreUserPersistenceAdapter adapter;

  private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

  @BeforeEach
  void cleanup() {
    dsl.deleteFrom(IGNORE_USER).execute();
  }

  @Test
  void save_and_load() {
    var user = IgnoreUser.create(new UserId(123L), new ChannelId(456L), "test", NOW);
    adapter.save(user);

    var loaded = adapter.loadByUserIdAndChannelId(new UserId(123L), new ChannelId(456L));
    assertThat(loaded).isPresent();
    assertThat(loaded.get().userId()).isEqualTo(new UserId(123L));
    assertThat(loaded.get().channelId()).isEqualTo(new ChannelId(456L));
    assertThat(loaded.get().name()).isEqualTo("test");
  }

  @Test
  void load_returns_empty_when_not_found() {
    assertThat(adapter.loadByUserIdAndChannelId(new UserId(999L), new ChannelId(999L))).isEmpty();
  }

  @Test
  void delete_removes_record() {
    var user = IgnoreUser.create(new UserId(123L), new ChannelId(456L), "test", NOW);
    adapter.save(user);

    var loaded = adapter.loadByUserIdAndChannelId(new UserId(123L), new ChannelId(456L)).get();
    adapter.delete(loaded);

    assertThat(adapter.loadByUserIdAndChannelId(new UserId(123L), new ChannelId(456L))).isEmpty();
  }
}
