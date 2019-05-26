package de.dk.ch;

import java.util.LinkedList;

/**
 * A listener that can be attached to a {@link Channel}.
 * This listener gets the messages that are received through the channel.
 *
 * @author David Koettlitz
 * <br>Erstellt am 14.07.2017
 */
public interface ChannelListener {
   /**
    * Handle the message that was received through the channel.
    *
    * @param data The received data
    */
   public void received(byte[] data);

   public static class ChannelListenerChain extends LinkedList<ChannelListener>
                                            implements ChannelListener {
      private static final long serialVersionUID = 1L;

      @Override
      public void received(byte[] data) {
         ChannelListener[] listeners = toArray(new ChannelListener[size()]);
         for (ChannelListener l : listeners)
            l.received(data);
      }
   }
}