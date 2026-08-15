package org.specter.converter.adapter.persistence.port;

import static org.specter.converter.adapter.persistence.generated.Tables.IGNORE_USER;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.Optional;
import org.jooq.DSLContext;
import org.specter.converter.application.port.output.LoadIgnoreUserPort;
import org.specter.converter.application.port.output.SaveIgnoreUserPort;
import org.specter.converter.domain.model.ChannelId;
import org.specter.converter.domain.model.IgnoreUser;
import org.specter.converter.domain.model.IgnoreUserId;
import org.specter.converter.domain.model.UserId;
import org.springframework.context.ApplicationEventPublisher;

public class IgnoreUserPersistenceAdapter implements LoadIgnoreUserPort, SaveIgnoreUserPort {

    private final DSLContext dsl;
    private final ApplicationEventPublisher eventPublisher;

    public IgnoreUserPersistenceAdapter(DSLContext dsl, ApplicationEventPublisher eventPublisher) {
        this.dsl = Objects.requireNonNull(dsl, "dsl");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher");
    }

    @Override
    public Optional<IgnoreUser> loadByUserIdAndChannelId(UserId userId, ChannelId channelId) {
        return dsl.selectFrom(IGNORE_USER)
                .where(IGNORE_USER.USER_ID.eq(userId.value()).and(IGNORE_USER.CHANNEL_ID.eq(channelId.value())))
                .fetchOptional()
                .map(r -> IgnoreUser.reconstitute(
                        new IgnoreUserId(r.getId()),
                        new UserId(r.getUserId()),
                        new ChannelId(r.getChannelId()),
                        r.getName(),
                        r.getCreatedAt().toInstant(),
                        r.getUpdatedAt().toInstant(),
                        r.getVersion()));
    }

    @Override
    public void save(IgnoreUser ignoreUser) {
        Objects.requireNonNull(ignoreUser, "ignoreUser");
        try {
            if (ignoreUser.id().equals(IgnoreUserId.UNSAVED)) {
                dsl.insertInto(IGNORE_USER)
                        .set(IGNORE_USER.USER_ID, ignoreUser.userId().value())
                        .set(IGNORE_USER.CHANNEL_ID, ignoreUser.channelId().value())
                        .set(IGNORE_USER.NAME, ignoreUser.name())
                        .set(IGNORE_USER.VERSION, ignoreUser.version())
                        .set(IGNORE_USER.CREATED_AT, OffsetDateTime.ofInstant(ignoreUser.createdAt(), ZoneOffset.UTC))
                        .set(IGNORE_USER.UPDATED_AT, OffsetDateTime.ofInstant(ignoreUser.updatedAt(), ZoneOffset.UTC))
                        .execute();
            } else {
                int affected = dsl.update(IGNORE_USER)
                        .set(IGNORE_USER.USER_ID, ignoreUser.userId().value())
                        .set(IGNORE_USER.CHANNEL_ID, ignoreUser.channelId().value())
                        .set(IGNORE_USER.NAME, ignoreUser.name())
                        .set(IGNORE_USER.VERSION, ignoreUser.version() + 1)
                        .set(IGNORE_USER.UPDATED_AT, OffsetDateTime.ofInstant(ignoreUser.updatedAt(), ZoneOffset.UTC))
                        .where(IGNORE_USER
                                .ID
                                .eq(ignoreUser.id().value())
                                .and(IGNORE_USER.VERSION.eq(ignoreUser.version())))
                        .execute();
                if (affected == 0) {
                    throw new OptimisticLockException(
                            "IgnoreUser id=" + ignoreUser.id().value() + " version=" + ignoreUser.version());
                }
            }
        } finally {
            ignoreUser.pullDomainEvents().forEach(eventPublisher::publishEvent);
        }
    }

    @Override
    public void delete(IgnoreUser ignoreUser) {
        Objects.requireNonNull(ignoreUser, "ignoreUser");
        int affected = dsl.deleteFrom(IGNORE_USER)
                .where(IGNORE_USER.ID.eq(ignoreUser.id().value()).and(IGNORE_USER.VERSION.eq(ignoreUser.version())))
                .execute();
        if (affected == 0) {
            throw new OptimisticLockException(
                    "IgnoreUser id=" + ignoreUser.id().value() + " version=" + ignoreUser.version());
        }
        ignoreUser.pullDomainEvents().forEach(eventPublisher::publishEvent);
    }
}
