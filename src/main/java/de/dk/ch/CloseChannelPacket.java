package de.dk.ch;

/**
 * @author David Koettlitz
 * <br>Erstellt am 07.08.2017
 */
public class CloseChannelPacket extends ChannelPacket {
   private static final long serialVersionUID = -8267538933859326659L;

   private final String msg;

   public CloseChannelPacket(long channelId, String msg) {
      super(channelId, ChannelPacketType.CLOSE);
      this.msg = msg;
   }

   public CloseChannelPacket(long channelId) {
      this(channelId, null);
   }

   public String getMsg() {
      return msg;
   }

   @Override
   public String toString() {
      return "CloseChannelPacket {channelID=" + channelId + ", msg=\"" + msg + "\"}";
   }
}
