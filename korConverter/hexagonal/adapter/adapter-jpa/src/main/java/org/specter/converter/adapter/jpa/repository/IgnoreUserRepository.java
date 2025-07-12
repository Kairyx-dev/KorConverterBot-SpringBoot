package org.specter.converter.adapter.jpa.repository;

import java.util.Optional;
import org.specter.converter.adapter.jpa.entity.IgnoreUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IgnoreUserRepository extends JpaRepository<IgnoreUserEntity, Long> {

  Optional<IgnoreUserEntity> findByUserIdAndChannelId(long userId, long channelId);

  void deleteByUserIdAndChannelId(long userId, long channelId);
}
