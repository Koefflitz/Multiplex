package de.dk.ch;

import java.io.Serializable;

public abstract class Packet implements Serializable {
   private static final long serialVersionUID = -6750454488616222885L;

   public final long channelId;

   public Packet(long channelId) {
      this.channelId = channelId;
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + (int) (this.channelId ^ (this.channelId >>> 32));
      return result;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj)
         return true;
      if (obj == null)
         return false;
      if (getClass() != obj.getClass())
         return false;
      Packet other = (Packet) obj;
      if (this.channelId != other.channelId)
         return false;
      return true;
   }

   @Override
   public abstract String toString();
}