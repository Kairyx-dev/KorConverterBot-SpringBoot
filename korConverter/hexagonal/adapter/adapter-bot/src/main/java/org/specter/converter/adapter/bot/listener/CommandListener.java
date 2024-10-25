package org.specter.converter.adapter.bot.listener;

import lombok.AllArgsConstructor;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.jetbrains.annotations.NotNull;
import org.specter.converter.adapter.bot.entity.BotCommand;
import org.specter.converter.adapter.bot.properties.BotProperties;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class CommandListener extends ListenerAdapter {
  private final BotProperties botProperties;

  @Override
  public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
    super.onSlashCommandInteraction(event);
    BotCommand command = BotCommand.fromCommand(event.getName());

    switch (command) {
      case ECO_VERSION -> onEcoVersion(event);
      case ECO_TEST -> onEcoTest(event);
      case UNKNOWN -> onUnknownCommand(event);
    }
  }

  private void onEcoTest(@NotNull SlashCommandInteractionEvent event){
    String ecoContents = event.getOption("content", OptionMapping::getAsString);

    if (ecoContents != null) {
      event.reply(ecoContents).queue();
    }
  }

  private void onEcoVersion(@NotNull SlashCommandInteractionEvent event) {
    String content = String.format("현재 bot의 버전은 v%s 입니다.", botProperties.getBotVersion());
    event.reply(content).queue();
  }

  private void onUnknownCommand(@NotNull SlashCommandInteractionEvent event) {
    event.reply("알수없는 명령어 입니다.").queue();
  }
}
