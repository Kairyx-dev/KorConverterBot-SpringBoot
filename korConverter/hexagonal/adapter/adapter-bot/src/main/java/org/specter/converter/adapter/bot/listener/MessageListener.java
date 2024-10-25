package org.specter.converter.adapter.bot.listener;

import java.awt.Color;
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
import org.specter.converter.aplication.inport.ConverterInPort;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@AllArgsConstructor
public class MessageListener extends ListenerAdapter {
  private final ConverterInPort converterInPort;


  @Override
  public void onMessageReceived(@NotNull MessageReceivedEvent event) {
    if (event.isFromType(ChannelType.PRIVATE)) {
      log.info("Message from Private Channel {}: {}", event.getAuthor().getName(),
          event.getMessage().getContentDisplay());
    } else {
      log.info("Original MSG [{}] [{}] {}: {}", event.getGuild().getName(),
          event.getChannel().getName(),
          event.getMember().getEffectiveName(),
          event.getMessage().getContentRaw());

      if (event.getAuthor().isBot()) {
        return;
      }

      String before = event.getMessage().getContentRaw();
      String after = converterInPort.engToKor(before);

      if (!converterInPort.checkAvailableStr(before)) {
        log.warn("UnParseable String {}", before);
        return;
      }

      User author = event.getMessage().getAuthor();
      String avatarUrl = author.getAvatarUrl() != null ?
          author.getAvatarUrl() :
          author.getDefaultAvatarUrl();

      String serverName =
          event.getMember().getUser().getEffectiveName() + "(" + event.getMember().getUser()
              .getName() + ")";

      log.info("Parsing: {} -> {}", before, after);
      try {
        event.getMessage().delete().complete();
        editEmbed(serverName, before, after, event);
      } catch (UnEditableMessageException e) {
        log.warn("Can not edit past message: {}", e.getMessage());
        sendEmbed(serverName, avatarUrl, before, after, event);
      }
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
      log.error("Error when send message", throwable);
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
      log.error("Error when editting message", throwable);
      return null;
    }).queue();
  }
}
