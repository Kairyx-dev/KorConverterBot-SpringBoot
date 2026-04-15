package org.specter.converter.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.specter.converter.domain.event.IgnoreUserAddedEvent;
import org.specter.converter.domain.event.IgnoreUserEvent;
import org.specter.converter.domain.event.IgnoreUserRemovedEvent;

public final class IgnoreUser {
    private final IgnoreUserId id;
    private final UserId userId;
    private final ChannelId channelId;
    private final String name;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final long version;
    private final List<IgnoreUserEvent> domainEvents = new ArrayList<>();

    private IgnoreUser(IgnoreUserId id, UserId userId, ChannelId channelId,
                       String name, Instant createdAt, Instant updatedAt, long version) {
        this.id = Objects.requireNonNull(id, "id");
        this.userId = Objects.requireNonNull(userId, "userId");
        this.channelId = Objects.requireNonNull(channelId, "channelId");
        this.name = Objects.requireNonNull(name, "name");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
        this.version = version;
    }

    public static IgnoreUser create(UserId userId, ChannelId channelId, String name, Instant now) {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(channelId, "channelId");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(now, "now");
        var user = new IgnoreUser(IgnoreUserId.UNSAVED, userId, channelId, name, now, now, 0L);
        user.registerEvent(new IgnoreUserAddedEvent(
                UUID.randomUUID(), "IGNORE_USER_ADDED", user.id.value(), now, user.version));
        return user;
    }

    public static IgnoreUser reconstitute(IgnoreUserId id, UserId userId, ChannelId channelId,
                                           String name, Instant createdAt, Instant updatedAt, long version) {
        return new IgnoreUser(id, userId, channelId, name, createdAt, updatedAt, version);
    }

    public void markForRemoval(Instant now) {
        Objects.requireNonNull(now, "now");
        registerEvent(new IgnoreUserRemovedEvent(
                UUID.randomUUID(), "IGNORE_USER_REMOVED", this.id.value(), now, this.version));
    }

    public List<IgnoreUserEvent> pullDomainEvents() {
        var events = List.copyOf(domainEvents);
        domainEvents.clear();
        return events;
    }

    private void registerEvent(IgnoreUserEvent event) {
        domainEvents.add(event);
    }

    public IgnoreUserId id() { return id; }
    public UserId userId() { return userId; }
    public ChannelId channelId() { return channelId; }
    public String name() { return name; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
    public long version() { return version; }
}
