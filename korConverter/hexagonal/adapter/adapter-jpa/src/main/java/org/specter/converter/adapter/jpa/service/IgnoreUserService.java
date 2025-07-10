package org.specter.converter.adapter.jpa.service;

import java.util.List;
import java.util.Optional;
import org.specter.converter.adapter.jpa.mapper.IgnoreUserMapper;
import org.specter.converter.adapter.jpa.repository.IgnoreUserRepository;
import org.specter.converter.domain.model.IgnoreUser;
import org.springframework.stereotype.Service;

@Service
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

  public Optional<IgnoreUser> findByUserId(long userId, long channelId) {
    return repository.findByUserIdAndChannelId(userId, channelId).map(mapper::toDomain);
  }

  public IgnoreUser save(IgnoreUser ignoreUser) {
    return mapper.toDomain(repository.save(mapper.toEntity(ignoreUser)));
  }

  public void deleteByUserId(long userId, long channelId) {
    repository.deleteByUserIdAndChannelId(userId, channelId);
  }
}
