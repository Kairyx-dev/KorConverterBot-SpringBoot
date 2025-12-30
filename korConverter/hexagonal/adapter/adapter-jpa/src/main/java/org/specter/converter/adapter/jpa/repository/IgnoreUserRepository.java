package org.specter.converter.adapter.jpa.repository;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.specter.converter.adapter.jpa.entity.IgnoreUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
@NullMarked
public interface IgnoreUserRepository extends JpaRepository<IgnoreUserEntity, Long> {

  @Nullable IgnoreUserEntity findByUserIdAndChannelId(long userId, long channelId);

  void deleteByUserIdAndChannelId(long userId, long channelId);
}
