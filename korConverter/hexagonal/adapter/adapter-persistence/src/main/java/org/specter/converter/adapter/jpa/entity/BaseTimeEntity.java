package org.specter.converter.adapter.jpa.entity;


import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.time.LocalDateTime;
import java.time.ZoneId;
import lombok.Getter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@MappedSuperclass
@Getter
abstract class BaseTimeEntity {

  @CreationTimestamp
  @Column(nullable = false, updatable = false)
  protected LocalDateTime createdAt = LocalDateTime.now(ZoneId.systemDefault());

  @UpdateTimestamp
  @Column(nullable = false)
  protected LocalDateTime updatedAt = LocalDateTime.now(ZoneId.systemDefault());
}
