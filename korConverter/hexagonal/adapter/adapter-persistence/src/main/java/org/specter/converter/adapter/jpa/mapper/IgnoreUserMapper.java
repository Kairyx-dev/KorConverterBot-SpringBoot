package org.specter.converter.adapter.jpa.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants.ComponentModel;
import org.specter.converter.adapter.jpa.entity.IgnoreUserEntity;
import org.specter.converter.domain.model.IgnoreUser;

@Mapper(componentModel = ComponentModel.SPRING)
public abstract class IgnoreUserMapper extends AbstractMapstructMapper<IgnoreUserEntity, IgnoreUser> {

}
