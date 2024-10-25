package org.specter.converter.adapter.bot.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "bot")
@Getter
@Setter
public class BotProperties {

  private String token;
}
