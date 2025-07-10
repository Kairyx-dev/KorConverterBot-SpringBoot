package org.specter.converter.adapter.jpa.entity;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.With;

@Entity
@Getter
@With
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "ignore_user", indexes = @Index(name = "idx_ignore_user_user_id", columnList = "userId, channelId"))
public class IgnoreUserEntity extends BaseTimeEntity {

  @Id
  @GeneratedValue
  private Long id;

  private String name;

  @Column(nullable = false)
  private long userId;

  @Column(nullable = false)
  private long channelId;

  @Builder
  public IgnoreUserEntity(Long id, String name, Long userId, Long ChannelId) {
    this.id = id;
    this.name = name;
    this.userId = userId;
    this.channelId = ChannelId;
  }
}
