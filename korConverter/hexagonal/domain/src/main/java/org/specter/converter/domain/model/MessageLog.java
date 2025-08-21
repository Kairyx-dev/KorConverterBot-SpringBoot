package org.specter.converter.domain.model;

import lombok.Builder;
import lombok.With;

@Builder
@With
public record MessageLog(
    Long id,
    String guild,
    String channel,
    String nickName,
    String effectiveName,
    String message,
    Boolean isConverted,
    String convertedMessage
) {

  public MessageLog {
    if (isConverted == null) {
      isConverted = false;
    }
  }

}
