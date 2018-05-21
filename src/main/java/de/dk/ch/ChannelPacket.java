package de.dk.ch;

import java.util.Objects;

/**
 * @author David Koettlitz
 * <br>Erstellt am 07.08.2017
 */
public class ChannelPacket extends Packet {
   private static final long serialVersionUID = 4075237550738947272L;

   private final ChannelPacketType type;

   public ChannelPacket(long channelId, ChannelPacketType type) {
      super(channelId);
      this.type = Objects.requireNonNull(type);
   }

   public ChannelPacketType getPacketType() {
      return type;
   }

   @Override
   public String toString() {
      return "ChannelPacket {channelID=" + channelId + ", type=" + type + "}";
   }

   public static enum ChannelPacketType {
      NEW,
      OK,
      REFUSED,
      CLOSE;
   }
}
