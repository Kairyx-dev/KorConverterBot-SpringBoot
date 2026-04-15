package org.specter.converter.domain.model;

import java.time.LocalDateTime;

public record IgnoreUser(
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    Long id,
    String name,
    long userId,
    long channelId
) {

}
