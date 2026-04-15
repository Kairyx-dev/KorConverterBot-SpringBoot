package org.specter.converter.adapter.jpa.repository;

import org.jspecify.annotations.NullMarked;
import org.specter.converter.adapter.jpa.entity.MessageLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
@NullMarked
public interface MessageLogRepository extends JpaRepository<MessageLogEntity, Long> {

}
