package org.specter.converter.domain.model;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.With;
import org.jspecify.annotations.NullMarked;

@With
@Builder
@NullMarked
public record IgnoreUser(
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    Long id,
    String name,
    long userId,
    long channelId
) {

}
