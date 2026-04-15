package org.specter.converter.application.port.output;

import java.util.Optional;
import org.specter.converter.domain.model.ChannelId;
import org.specter.converter.domain.model.IgnoreUser;
import org.specter.converter.domain.model.UserId;

public interface LoadIgnoreUserPort {
  Optional<IgnoreUser> loadByUserIdAndChannelId(UserId userId, ChannelId channelId);
}
