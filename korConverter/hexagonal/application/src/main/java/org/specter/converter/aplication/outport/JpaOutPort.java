package org.specter.converter.aplication.outport;

import java.util.List;
import java.util.Optional;
import org.specter.converter.domain.model.IgnoreUser;
import org.specter.converter.domain.model.MessageLog;

public interface JpaOutPort {

  List<IgnoreUser> findAllIgnoreUsers();

  Optional<IgnoreUser> findIgnoreUser(long userId, long channelId);

  IgnoreUser saveIgnoreUser(IgnoreUser ignoreUser);

  MessageLog saveMessageLog(MessageLog messageLog);

  void deleteIgnoreUser(long userId, long channelId);
}
