package org.specter.converter.adapter.jpa.service;

import lombok.AllArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.specter.converter.adapter.jpa.mapper.MessageLogMapper;
import org.specter.converter.adapter.jpa.repository.MessageLogRepository;
import org.specter.converter.domain.model.MessageLog;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@NullMarked
public class MessageLogService {

  private final MessageLogMapper mapper;
  private final MessageLogRepository repository;

  public MessageLog save(MessageLog domain) {
    return mapper.toDomain(repository.save(mapper.toEntity(domain)));
  }
}
