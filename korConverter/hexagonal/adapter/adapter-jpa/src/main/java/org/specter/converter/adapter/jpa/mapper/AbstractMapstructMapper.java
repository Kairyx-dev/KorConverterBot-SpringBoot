package org.specter.converter.adapter.jpa.mapper;

import java.util.List;

public abstract class AbstractMapstructMapper<E, D> implements MapstructMapper<E, D> {

  @Override
  public List<D> toDomainList(List<E> entityList) {
    return entityList.stream().map(this::toDomain).toList();
  }

  @Override
  public List<E> toEntityList(List<D> domainList) {
    return domainList.stream().map(this::toEntity).toList();
  }
}
