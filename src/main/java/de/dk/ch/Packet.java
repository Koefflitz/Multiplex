package de.dk.ch;

import java.io.Serializable;
import java.util.Objects;

public abstract class Packet implements Serializable {
   private static final long serialVersionUID = -6750454488616222885L;

   public final byte channelId;

   public Packet(byte channelId) {
      this.channelId = channelId;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Packet packet = (Packet) o;
      return channelId == packet.channelId;
   }

   @Override
   public int hashCode() {
      return Objects.hash(channelId);
   }

   @Override
   public abstract String toString();
}