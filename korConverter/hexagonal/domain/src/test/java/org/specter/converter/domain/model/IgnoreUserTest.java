package org.specter.converter.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.specter.converter.domain.event.IgnoreUserAddedEvent;
import org.specter.converter.domain.event.IgnoreUserRemovedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class IgnoreUserTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final UserId USER_ID = new UserId(123L);
    private static final ChannelId CHANNEL_ID = new ChannelId(456L);
    private static final String NAME = "testUser";

    @Test
    @DisplayName("create emits IgnoreUserAddedEvent")
    void create_emits_added_event() {
        var user = IgnoreUser.create(USER_ID, CHANNEL_ID, NAME, NOW);
        var events = user.pullDomainEvents();

        assertThat(events).hasSize(1);
        assertThat(events.getFirst()).isInstanceOf(IgnoreUserAddedEvent.class);

        var event = (IgnoreUserAddedEvent) events.getFirst();
        assertThat(event.eventType()).isEqualTo("IGNORE_USER_ADDED");
        assertThat(event.occurredAt()).isEqualTo(NOW);
        assertThat(event.aggregateVersion()).isEqualTo(0L);
        assertThat(event.eventId()).isNotNull();
    }

    @Test
    @DisplayName("create sets UNSAVED id and correct fields")
    void create_sets_unsaved_id() {
        var user = IgnoreUser.create(USER_ID, CHANNEL_ID, NAME, NOW);

        assertThat(user.id()).isEqualTo(IgnoreUserId.UNSAVED);
        assertThat(user.userId()).isEqualTo(USER_ID);
        assertThat(user.channelId()).isEqualTo(CHANNEL_ID);
        assertThat(user.name()).isEqualTo(NAME);
        assertThat(user.createdAt()).isEqualTo(NOW);
        assertThat(user.updatedAt()).isEqualTo(NOW);
        assertThat(user.version()).isEqualTo(0L);
    }

    @Test
    @DisplayName("reconstitute does not emit events")
    void reconstitute_does_not_emit_events() {
        var id = new IgnoreUserId(1L);
        var user = IgnoreUser.reconstitute(id, USER_ID, CHANNEL_ID, NAME, NOW, NOW, 3L);

        assertThat(user.pullDomainEvents()).isEmpty();
        assertThat(user.version()).isEqualTo(3L);
        assertThat(user.id()).isEqualTo(id);
    }

    @Test
    @DisplayName("pullDomainEvents clears after call")
    void pullDomainEvents_clears_after_call() {
        var user = IgnoreUser.create(USER_ID, CHANNEL_ID, NAME, NOW);

        var first = user.pullDomainEvents();
        assertThat(first).hasSize(1);

        var second = user.pullDomainEvents();
        assertThat(second).isEmpty();
    }

    @Test
    @DisplayName("markForRemoval emits IgnoreUserRemovedEvent")
    void markForRemoval_emits_removed_event() {
        var id = new IgnoreUserId(10L);
        var user = IgnoreUser.reconstitute(id, USER_ID, CHANNEL_ID, NAME, NOW, NOW, 2L);
        var removalTime = Instant.parse("2026-06-15T12:00:00Z");

        user.markForRemoval(removalTime);
        var events = user.pullDomainEvents();

        assertThat(events).hasSize(1);
        assertThat(events.getFirst()).isInstanceOf(IgnoreUserRemovedEvent.class);

        var event = (IgnoreUserRemovedEvent) events.getFirst();
        assertThat(event.eventType()).isEqualTo("IGNORE_USER_REMOVED");
        assertThat(event.aggregateId()).isEqualTo(10L);
        assertThat(event.occurredAt()).isEqualTo(removalTime);
        assertThat(event.aggregateVersion()).isEqualTo(2L);
    }
}
