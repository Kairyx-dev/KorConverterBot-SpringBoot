package org.specter.converter.adapter.bot.listener;

import java.awt.Color;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import org.jetbrains.annotations.NotNull;
import org.specter.converter.adapter.bot.entity.UnEditableMessageException;
import org.specter.converter.aplication.inport.DiscordBotInPort;
import org.specter.converter.domain.model.IgnoreUser;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@AllArgsConstructor
public class MessageListener extends ListenerAdapter {

  private final DiscordBotInPort discordBotInPort;

  @Override
  public void onMessageReceived(@NotNull MessageReceivedEvent event) {
    log.atInfo()
        .addKeyValue("guild", event.getGuild().getName())
        .addKeyValue("channel", event.getChannel().getName())
        .addKeyValue("member.effectiveName", event.getMember().getEffectiveName())
        .addKeyValue("content", event.getMessage().getContentRaw())
        .log("Original Message");

    if (checkConvertable(event)) {
      convertAndSend(event);
    }
  }

  private boolean checkConvertable(@NotNull MessageReceivedEvent event) {
    if (event.isFromType(ChannelType.PRIVATE)) {
      log.atInfo()
          .addKeyValue("author", event.getAuthor().getName())
          .addKeyValue("content", event.getMessage().getContentDisplay())
          .log("Message from Private Channel");
      return false;
    }

    if (event.getAuthor().isBot()) {
      return false;
    }

    long userId = event.getAuthor().getIdLong();
    long channelId = event.getChannel().getIdLong();

    if (discordBotInPort.checkIgnoredUser(userId, channelId)) {
      log.atInfo()
          .addKeyValue("userId", userId)
          .addKeyValue("channelId", channelId)
          .log("this is ignored user");
      return false;
    }

    String before = event.getMessage().getContentRaw();

    if (!discordBotInPort.checkAvailableStr(before)) {
      log.atInfo()
          .addKeyValue("before", before)
          .log("UnParseable String");
      return false;
    }

    return true;
  }

  private void convertAndSend(@NotNull MessageReceivedEvent event) {
    String before = event.getMessage().getContentRaw();
    String after = discordBotInPort.engToKor(before);

    User author = event.getMessage().getAuthor();
    String avatarUrl = Optional.ofNullable(author.getAvatarUrl()).orElse(author.getDefaultAvatarUrl());
    String authorName = "%s (%s)".formatted(author.getEffectiveName(), author.getName());

    log.atInfo()
        .addKeyValue("before", before)
        .addKeyValue("after", after)
        .log("Parsing");
    try {
      event.getMessage().delete().complete();
      editEmbed(authorName, before, after, event);
    } catch (UnEditableMessageException e) {
      log.atWarn()
          .addKeyValue("cause.message", e.getMessage())
          .log("Can not edit past message");
      sendEmbed(authorName, avatarUrl, before, after, event);
    }
  }

  private void sendEmbed(String authorName, String avatarUrl, String before, String after,
      MessageReceivedEvent event) {
    MessageEmbed embed = new EmbedBuilder()
        .setAuthor(authorName, null, avatarUrl)
        .addField(before, after, false)
        .setColor(new Color(255, 216, 228))
        .build();
    MessageCreateData data = new MessageCreateBuilder()
        .setEmbeds(embed)
        .build();

    event.getChannel().sendMessage(data).onErrorMap(throwable -> {

      log.atError()
          .setCause(throwable)
          .addKeyValue("after", after)
          .addKeyValue("authorName", authorName)
          .addKeyValue("avatarUrl", avatarUrl)
          .log("Error when send message");
      return null;
    }).queue();
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
    if (!existEmbed.getAuthor().getName().equals(authorName)) {
      throw UnEditableMessageException.diffName();
    }

    MessageEmbed embed = new EmbedBuilder(existEmbed)
        .addField(before, after, false)
        .build();
    MessageEditData data = new MessageEditBuilder()
        .setEmbeds(embed)
        .build();

    message.editMessage(data).onErrorMap(throwable -> {

      log.atError()
          .setCause(throwable)
          .addKeyValue("after", after)
          .addKeyValue("authorName", authorName)
          .log("Error when send message");
      return null;
    }).queue();
  }
}
