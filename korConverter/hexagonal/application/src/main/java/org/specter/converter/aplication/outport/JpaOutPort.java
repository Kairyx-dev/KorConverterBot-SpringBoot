package org.specter.converter.aplication.outport;

import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.specter.converter.domain.model.IgnoreUser;
import org.specter.converter.domain.model.MessageLog;

@NullMarked
public interface JpaOutPort {

  List<IgnoreUser> findAllIgnoreUsers();

  @Nullable IgnoreUser findIgnoreUser(long userId, long channelId);

  IgnoreUser saveIgnoreUser(IgnoreUser ignoreUser);

  MessageLog saveMessageLog(MessageLog messageLog);

  void deleteIgnoreUser(long userId, long channelId);
}
