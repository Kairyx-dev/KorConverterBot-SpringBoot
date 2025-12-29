package org.specter.converter.adapter.jpa.mapper;

import java.util.List;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface MapstructMapper<E, D> {

    D toDomain(E entity);

    E toEntity(D dto);

    List<D> toDomainList(List<E> entityList);

    List<E> toEntityList(List<D> domainList);
}
