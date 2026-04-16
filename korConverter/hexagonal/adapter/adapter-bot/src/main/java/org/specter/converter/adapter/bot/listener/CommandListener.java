package org.specter.converter.adapter.bot.listener;

import java.util.Objects;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.specter.converter.adapter.bot.entity.BotCommand;
import org.specter.converter.application.dto.command.AddIgnoreUserCommand;
import org.specter.converter.application.dto.command.RemoveIgnoreUserCommand;
import org.specter.converter.application.dto.result.IgnoreUserResult;
import org.specter.converter.application.port.input.AddIgnoreUserUseCase;
import org.specter.converter.application.port.input.RemoveIgnoreUserUseCase;
import org.springframework.boot.info.BuildProperties;

@NullMarked
public class CommandListener extends ListenerAdapter {

  private static final Logger log = LoggerFactory.getLogger(CommandListener.class);

  private final AddIgnoreUserUseCase addIgnoreUserUseCase;
  private final RemoveIgnoreUserUseCase removeIgnoreUserUseCase;
  private final BuildProperties buildProperties;

  public CommandListener(
      AddIgnoreUserUseCase addIgnoreUserUseCase,
      RemoveIgnoreUserUseCase removeIgnoreUserUseCase,
      BuildProperties buildProperties) {
    this.addIgnoreUserUseCase =
        Objects.requireNonNull(addIgnoreUserUseCase, "addIgnoreUserUseCase");
    this.removeIgnoreUserUseCase =
        Objects.requireNonNull(removeIgnoreUserUseCase, "removeIgnoreUserUseCase");
    this.buildProperties = Objects.requireNonNull(buildProperties, "buildProperties");
  }

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
    try {
      IgnoreUserResult result =
          addIgnoreUserUseCase.execute(
              new AddIgnoreUserCommand(
                  event.getUser().getIdLong(),
                  event.getChannelIdLong(),
                  getNickNameOrUserName(event)));

      log.atInfo().addKeyValue("ignored", result).log("User ignored");

      event.reply(result.name() + "님의 메시지가 무시됩니다.").queue();
    } catch (RuntimeException e) {
      log.atWarn()
          .setCause(e)
          .addKeyValue("userId", event.getUser().getIdLong())
          .addKeyValue("channelId", event.getChannelIdLong())
          .log("Failed to add ignore user");
      event.reply("이미 무시 목록에 등록되어 있습니다.").setEphemeral(true).queue();
    }
  }

  private void onUnIgnoreMe(SlashCommandInteractionEvent event) {
    try {
      removeIgnoreUserUseCase.execute(
          new RemoveIgnoreUserCommand(event.getUser().getIdLong(), event.getChannelIdLong()));

      log.atInfo()
          .addKeyValue("userId", event.getUser().getIdLong())
          .addKeyValue("channelId", event.getChannelIdLong())
          .log("remove ignore user");

      event.reply(getNickNameOrUserName(event) + "님의 메시지 무시가 취소되었습니다.").queue();
    } catch (RuntimeException e) {
      log.atWarn()
          .setCause(e)
          .addKeyValue("userId", event.getUser().getIdLong())
          .addKeyValue("channelId", event.getChannelIdLong())
          .log("Failed to remove ignore user");
      event.reply("무시 목록에 등록되어 있지 않습니다.").setEphemeral(true).queue();
    }
  }

  private String getNickNameOrUserName(SlashCommandInteractionEvent event) {
    return event.getMember() != null
        ? event.getMember().getEffectiveName()
        : event.getUser().getEffectiveName();
  }
}
