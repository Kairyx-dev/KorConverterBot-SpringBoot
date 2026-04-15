package org.specter.converter.domain.event;

import java.time.Instant;
import java.util.UUID;

public sealed interface IgnoreUserEvent permits IgnoreUserAddedEvent, IgnoreUserRemovedEvent {
  UUID eventId();

  String eventType();

  long aggregateId();

  Instant occurredAt();

  long aggregateVersion();
}
