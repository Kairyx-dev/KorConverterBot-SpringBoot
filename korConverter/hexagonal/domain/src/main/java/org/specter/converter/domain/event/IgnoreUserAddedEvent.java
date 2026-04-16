package org.specter.converter.domain.event;

import java.time.Instant;
import java.util.UUID;

public record IgnoreUserAddedEvent(
    UUID eventId, String eventType, long aggregateId, Instant occurredAt, long aggregateVersion)
    implements IgnoreUserEvent {}
