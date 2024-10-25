package org.specter.converter.adapter.bot.entity;

public class UnEditableMessageException extends Exception {

  public UnEditableMessageException(String message) {
    super(message);
  }

  public static UnEditableMessageException diffName() {
    return new UnEditableMessageException("Not Equal Author");
  }

  public static UnEditableMessageException notEmbed() {
    return new UnEditableMessageException("Not Embed Message");
  }

  public static UnEditableMessageException notMyBot() {
    return new UnEditableMessageException("This is not my message");
  }
}
