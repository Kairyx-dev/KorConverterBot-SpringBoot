package org.specter.converter.application.port.output;

import java.util.List;
import org.specter.converter.application.dto.result.IgnoreUserResult;

public interface IgnoreUserQueryPort {
    boolean existsByUserIdAndChannelId(long userId, long channelId);

    List<IgnoreUserResult> findAllByChannelId(long channelId);
}
