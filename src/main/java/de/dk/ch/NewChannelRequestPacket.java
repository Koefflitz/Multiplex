package de.dk.ch;

import static de.dk.ch.ChannelPacket.ChannelPacketType.NEW;

import java.util.Optional;

/**
 * @author David Koettlitz
 * <br>Erstellt am 07.08.2017
 */
public class NewChannelRequestPacket extends ChannelPacket {
   private static final long serialVersionUID = -1974870581458480869L;

   private final Class<?> type;
   private Object initialMessage;

   public NewChannelRequestPacket(long channelId, Class<?> type, Object initialMsg) {
      super(channelId, NEW);
      this.type = type;
      this.initialMessage = initialMsg;
   }

   public NewChannelRequestPacket(long channelId, Class<?> type) {
      this(channelId, type, null);
   }

   public boolean hasInitialMessage() {
      return initialMessage != null;
   }

   public Optional<Object> getInitialMessage() {
      return Optional.ofNullable(initialMessage);
   }

   public Class<?> getType() {
      return type;
   }

   @Override
   public String toString() {
      return "NewChannelRequestPacket {channelID=" + channelId + ", type=" + type.getName() + "}";
   }
}
