package org.specter.converter.adapter.jpa.mapper;

import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public interface MapstructMapper<E, D> {

    @Nullable D toNullableDomain(@Nullable E entity);
    D toDomain(E entity);

    @Nullable E toNullableEntity(@Nullable D dto);
    E toEntity(D dto);

    List<D> toDomainList(List<E> entityList);

    List<E> toEntityList(List<D> domainList);
}
