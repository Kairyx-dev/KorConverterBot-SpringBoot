package org.specter.converter.adapter.jpa.mapper;

import org.mapstruct.Mapper;
import org.specter.converter.adapter.jpa.entity.IgnoreUserEntity;
import org.specter.converter.domain.model.IgnoreUser;

@Mapper(componentModel = "spring")
public abstract class IgnoreUserMapper extends AbstractMapstructMapper<IgnoreUserEntity, IgnoreUser> {

}
