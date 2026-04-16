package org.specter.converter.adapter.persistence.port;

import static org.specter.converter.adapter.persistence.generated.Tables.IGNORE_USER;

import java.util.List;
import java.util.Objects;
import org.jooq.DSLContext;
import org.specter.converter.application.dto.result.IgnoreUserResult;
import org.specter.converter.application.port.output.IgnoreUserQueryPort;

public class IgnoreUserQueryAdapter implements IgnoreUserQueryPort {

  private final DSLContext dsl;

  public IgnoreUserQueryAdapter(DSLContext dsl) {
    this.dsl = Objects.requireNonNull(dsl, "dsl");
  }

  @Override
  public boolean existsByUserIdAndChannelId(long userId, long channelId) {
    return dsl.fetchExists(
        dsl.selectFrom(IGNORE_USER)
            .where(IGNORE_USER.USER_ID.eq(userId).and(IGNORE_USER.CHANNEL_ID.eq(channelId))));
  }

  @Override
  public List<IgnoreUserResult> findAllByChannelId(long channelId) {
    return dsl.selectFrom(IGNORE_USER)
        .where(IGNORE_USER.CHANNEL_ID.eq(channelId))
        .fetch(r -> new IgnoreUserResult(r.getId(), r.getUserId(), r.getChannelId(), r.getName()));
  }
}
