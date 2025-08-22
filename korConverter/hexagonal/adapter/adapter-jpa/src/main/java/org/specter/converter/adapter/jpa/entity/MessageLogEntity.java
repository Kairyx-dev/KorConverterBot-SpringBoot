package org.specter.converter.adapter.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.With;
import org.hibernate.annotations.ColumnDefault;

@With
@Getter
@Entity
@Table(name = "message_log")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MessageLogEntity extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  private Long id;
  private String guild;
  private String channel;
  private String nickName;
  private String effectiveName;
  private String message;

  @Column(nullable = false)
  @ColumnDefault(value = "false")
  private Boolean isConverted;
  private String convertedMessage;

  @Builder
  public MessageLogEntity(Long id, String guild, String channel, String nickName, String effectiveName, String message,
      Boolean isConverted, String convertedMessage) {
    this.id = id;
    this.guild = guild;
    this.channel = channel;
    this.nickName = nickName;
    this.effectiveName = effectiveName;
    this.message = message;
    this.isConverted = isConverted;
    this.convertedMessage = convertedMessage;
  }
}
