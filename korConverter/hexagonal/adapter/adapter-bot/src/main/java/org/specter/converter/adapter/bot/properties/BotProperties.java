package org.specter.converter.adapter.bot.properties;

import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.NullMarked;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "bot")
@NullMarked
@Validated
public record BotProperties(
    @NotNull String token
) {

}
