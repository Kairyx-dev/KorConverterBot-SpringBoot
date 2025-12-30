package org.specter.converter.adapter.jpa.service;

import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.specter.converter.adapter.jpa.mapper.IgnoreUserMapper;
import org.specter.converter.adapter.jpa.repository.IgnoreUserRepository;
import org.specter.converter.domain.model.IgnoreUser;
import org.springframework.stereotype.Service;

@Service
@NullMarked
public class IgnoreUserService {

  private final IgnoreUserRepository repository;
  private final IgnoreUserMapper mapper;

  public IgnoreUserService(IgnoreUserRepository ignoreUserRepository, IgnoreUserMapper mapper) {
    this.repository = ignoreUserRepository;
    this.mapper = mapper;
  }

  public List<IgnoreUser> findAll() {
    return mapper.toDomainList(repository.findAll());
  }

  public @Nullable IgnoreUser findByUserId(long userId, long channelId) {
    return mapper.toNullableDomain(repository.findByUserIdAndChannelId(userId, channelId));
  }

  public IgnoreUser save(IgnoreUser ignoreUser) {
    return mapper.toDomain(repository.save(mapper.toEntity(ignoreUser)));
  }

  public void deleteByUserId(long userId, long channelId) {
    repository.deleteByUserIdAndChannelId(userId, channelId);
  }
}
