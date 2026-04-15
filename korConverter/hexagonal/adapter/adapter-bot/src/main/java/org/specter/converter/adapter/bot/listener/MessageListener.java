package org.specter.converter.adapter.bot.listener;

import java.awt.Color;
import java.util.Objects;
import java.util.Optional;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.MessageHistory;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import net.dv8tion.jda.api.utils.messages.MessageEditData;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.specter.converter.adapter.bot.entity.UnEditableMessageException;
import org.specter.converter.application.dto.command.ConvertMessageCommand;
import org.specter.converter.application.dto.query.CheckIgnoreUserQuery;
import org.specter.converter.application.dto.result.ConvertMessageResult;
import org.specter.converter.application.port.input.CheckIgnoreUserUseCase;
import org.specter.converter.application.port.input.ConvertMessageUseCase;

@NullMarked
public class MessageListener extends ListenerAdapter {

  private static final Logger log = LoggerFactory.getLogger(MessageListener.class);

  private final ConvertMessageUseCase convertMessageUseCase;
  private final CheckIgnoreUserUseCase checkIgnoreUserUseCase;

  public MessageListener(
      ConvertMessageUseCase convertMessageUseCase, CheckIgnoreUserUseCase checkIgnoreUserUseCase) {
    this.convertMessageUseCase =
        Objects.requireNonNull(convertMessageUseCase, "convertMessageUseCase");
    this.checkIgnoreUserUseCase =
        Objects.requireNonNull(checkIgnoreUserUseCase, "checkIgnoreUserUseCase");
  }

  @Override
  public void onMessageReceived(MessageReceivedEvent event) {
    log.atInfo()
        .addKeyValue("guild", event.getGuild().getName())
        .addKeyValue("channel", event.getChannel().getName())
        .addKeyValue("nickName", getNickNameOrUserName(event))
        .addKeyValue("effectiveName", event.getAuthor().getEffectiveName())
        .addKeyValue("content", event.getMessage().getContentRaw())
        .log("Original Message");

    if (!isConvertableEvent(event)) {
      return;
    }

    convertAndSend(event);
  }

  private boolean isConvertableEvent(MessageReceivedEvent event) {
    if (event.isFromType(ChannelType.PRIVATE)) {
      log.atInfo()
          .addKeyValue("author.nickname", getNickNameOrUserName(event))
          .addKeyValue("author.effectiveName", event.getAuthor().getEffectiveName())
          .addKeyValue("content", event.getMessage().getContentDisplay())
          .log("Message from Private Channel");
      return false;
    }

    if (event.getAuthor().isBot()) {
      return false;
    }

    long userId = event.getAuthor().getIdLong();
    long channelId = event.getChannel().getIdLong();

    if (checkIgnoreUserUseCase.execute(new CheckIgnoreUserQuery(userId, channelId))) {
      log.atInfo()
          .addKeyValue("userId", userId)
          .addKeyValue("channelId", channelId)
          .log("this is ignored user");
      return false;
    }

    return true;
  }

  private void convertAndSend(MessageReceivedEvent event) {
    String before = event.getMessage().getContentRaw();

    ConvertMessageResult result =
        convertMessageUseCase.execute(
            new ConvertMessageCommand(
                before,
                event.getGuild().getIdLong(),
                event.getChannel().getIdLong(),
                getNickNameOrUserName(event),
                event.getAuthor().getEffectiveName()));

    if (!result.converted()) {
      return;
    }

    String after = result.convertedMessage();
    User author = event.getMessage().getAuthor();
    String avatarUrl =
        Optional.ofNullable(author.getAvatarUrl()).orElse(author.getDefaultAvatarUrl());
    String authorName =
        "%s (%s)".formatted(getNickNameOrUserName(event), author.getEffectiveName());

    log.atInfo().addKeyValue("before", before).addKeyValue("after", after).log("Parsing");
    try {
      event.getMessage().delete().complete();
      editEmbed(authorName, before, after, event);
    } catch (UnEditableMessageException e) {
      log.atInfo().addKeyValue("cause.message", e.getMessage()).log("Can not edit past message");
      sendEmbed(authorName, avatarUrl, before, after, event);
    }
  }

  private void sendEmbed(
      String authorName,
      String avatarUrl,
      String before,
      String after,
      MessageReceivedEvent event) {
    MessageEmbed embed =
        new EmbedBuilder()
            .setAuthor(authorName, null, avatarUrl)
            .addField(before, after, false)
            .setColor(new Color(255, 216, 228))
            .build();
    MessageCreateData data = new MessageCreateBuilder().setEmbeds(embed).build();

    event
        .getChannel()
        .sendMessage(data)
        .onErrorMap(
            throwable -> {
              log.atError()
                  .setCause(throwable)
                  .addKeyValue("after", after)
                  .addKeyValue("authorName", authorName)
                  .addKeyValue("avatarUrl", avatarUrl)
                  .log("Error when send message");
              return null;
            })
        .queue();
  }

  private void editEmbed(String authorName, String before, String after, MessageReceivedEvent event)
      throws UnEditableMessageException {
    MessageHistory history = event.getChannel().getHistory();
    Message message = history.retrievePast(5).complete().getFirst();

    boolean isMyBot = event.getJDA().getSelfUser().getId().equals(message.getAuthor().getId());

    if (message.getEmbeds().isEmpty()) {
      throw UnEditableMessageException.notEmbed();
    }

    if (!isMyBot) {
      throw UnEditableMessageException.notMyBot();
    }

    MessageEmbed existEmbed = message.getEmbeds().getFirst();
    if (Optional.ofNullable(existEmbed.getAuthor())
        .map(MessageEmbed.AuthorInfo::getName)
        .filter(name -> name.equals(authorName))
        .isEmpty()) {
      throw UnEditableMessageException.diffName();
    }

    MessageEmbed embed = new EmbedBuilder(existEmbed).addField(before, after, false).build();
    MessageEditData data = new MessageEditBuilder().setEmbeds(embed).build();

    message
        .editMessage(data)
        .onErrorMap(
            throwable -> {
              log.atError()
                  .setCause(throwable)
                  .addKeyValue("after", after)
                  .addKeyValue("authorName", authorName)
                  .log("Error when send message");
              return null;
            })
        .queue();
  }

  private String getNickNameOrUserName(MessageReceivedEvent event) {
    return event.getMember() != null
        ? event.getMember().getEffectiveName()
        : event.getAuthor().getEffectiveName();
  }
}
