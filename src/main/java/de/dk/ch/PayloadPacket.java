package de.dk.ch;

/**
 * @author David Koettlitz
 * <br>Erstellt am 07.08.2017
 */
public class PayloadPacket extends Packet {
   private static final long serialVersionUID = 3100740666396486617L;

   private final Object payload;

   public PayloadPacket(long channelId, Object payload) {
      super(channelId);
      this.payload = payload;
   }

   public Object getPayload() {
      return payload;
   }

   @Override
   public String toString() {
      return "PayloadPacket { channelID=" + channelId + ", payload=" + payload + "}";
   }
}
