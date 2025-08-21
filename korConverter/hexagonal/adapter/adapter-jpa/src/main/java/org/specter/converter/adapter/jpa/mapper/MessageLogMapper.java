package org.specter.converter.adapter.jpa.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants.ComponentModel;
import org.specter.converter.adapter.jpa.entity.MessageLogEntity;
import org.specter.converter.domain.model.MessageLog;

@Mapper(componentModel = ComponentModel.SPRING)
public abstract class MessageLogMapper extends AbstractMapstructMapper<MessageLogEntity, MessageLog> {

}
