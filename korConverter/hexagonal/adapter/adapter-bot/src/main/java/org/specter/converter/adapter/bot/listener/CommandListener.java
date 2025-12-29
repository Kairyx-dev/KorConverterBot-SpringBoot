package org.specter.converter.adapter.bot.listener;

import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.jspecify.annotations.NullMarked;
import org.specter.converter.adapter.bot.entity.BotCommand;
import org.specter.converter.adapter.bot.properties.BotProperties;
import org.specter.converter.aplication.inport.DiscordBotInPort;
import org.specter.converter.domain.model.IgnoreUser;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@Slf4j
@NullMarked
public class CommandListener extends ListenerAdapter {

  private final DiscordBotInPort discordBotInPort;
  private final BuildProperties buildProperties;

  @Override
  public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
    super.onSlashCommandInteraction(event);
    log.info("Slash command coming {}", event.getName());
    BotCommand command = BotCommand.fromCommand(event.getName());

    switch (command) {
      case ECO_VERSION -> onEcoVersion(event);
      case ECO_TEST -> onEcoTest(event);
      case UNKNOWN -> onUnknownCommand(event);
      case IGNORE_ME -> onIgnoreMe(event);
      case UN_IGNORE_ME -> onUnIgnoreMe(event);
    }
  }

  private void onEcoTest(SlashCommandInteractionEvent event) {
    String ecoContents = event.getOption("content", OptionMapping::getAsString);

    event.reply(Objects.requireNonNullElse(ecoContents, "이 명령어는 메시지가 필요합니다.")).queue();
  }

  private void onEcoVersion(SlashCommandInteractionEvent event) {
    String content = String.format("현재 bot의 버전은 v%s 입니다.", buildProperties.getVersion());
    event.reply(content).queue();
  }

  private void onUnknownCommand(SlashCommandInteractionEvent event) {
    event.reply("알수없는 명령어 입니다.").queue();
  }

  private void onIgnoreMe(SlashCommandInteractionEvent event) {
    IgnoreUser saved = discordBotInPort.addIgnoreUser(IgnoreUser.builder()
        .userId(event.getUser().getIdLong())
        .name(getNickNameOrUserName(event))
        .channelId(event.getChannelIdLong())
        .build());

    log.atInfo()
        .addKeyValue("ignored", saved)
        .log("User ignored");

    event.reply(saved.name() + "님의 메시지가 무시됩니다.").queue();
  }

  private void onUnIgnoreMe(SlashCommandInteractionEvent event) {
    discordBotInPort.removeIgnoreUser(event.getUser().getIdLong(), event.getChannelIdLong());

    log.atInfo()
        .addKeyValue("userId", event.getUser().getIdLong())
        .addKeyValue("channelId", event.getChannelIdLong())
        .log("remove ignore user");

    event.reply(getNickNameOrUserName(event) + "님의 메시지 무시가 취소되었습니다.").queue();
  }

  private String getNickNameOrUserName(SlashCommandInteractionEvent event) {
    return event.getMember() != null ? event.getMember().getEffectiveName() : event.getUser().getEffectiveName();
  }
}
