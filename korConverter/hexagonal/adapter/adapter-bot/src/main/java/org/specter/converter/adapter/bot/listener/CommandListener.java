package org.specter.converter.adapter.bot.listener;

import jakarta.annotation.Nonnull;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.specter.converter.adapter.bot.entity.BotCommand;
import org.specter.converter.adapter.bot.properties.BotProperties;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@Slf4j
public class CommandListener extends ListenerAdapter {
  private final BotProperties botProperties;

  @Override
  public void onSlashCommandInteraction(@Nonnull SlashCommandInteractionEvent event) {
    super.onSlashCommandInteraction(event);
    log.info("Slash command coming {}", event.getName());
    BotCommand command = BotCommand.fromCommand(event.getName());

    switch (command) {
      case ECO_VERSION -> onEcoVersion(event);
      case ECO_TEST -> onEcoTest(event);
      case UNKNOWN -> onUnknownCommand(event);
    }
  }

  private void onEcoTest(@Nonnull SlashCommandInteractionEvent event){
    String ecoContents = event.getOption("content", OptionMapping::getAsString);

    event.reply(Objects.requireNonNullElse(ecoContents, "이 명령어는 메시지가 필요합니다.")).queue();
  }

  private void onEcoVersion(@Nonnull SlashCommandInteractionEvent event) {
    String content = String.format("현재 bot의 버전은 v%s 입니다.", botProperties.getBotVersion());
    event.reply(content).queue();
  }

  private void onUnknownCommand(@Nonnull SlashCommandInteractionEvent event) {
    event.reply("알수없는 명령어 입니다.").queue();
  }
}
