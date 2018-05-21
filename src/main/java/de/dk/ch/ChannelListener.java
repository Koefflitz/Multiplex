package de.dk.ch;

import java.util.LinkedList;

/**
 * A listener that can be attached to a {@link Channel}.
 * This listener gets the messages that are received through the channel.
 *
 * @author David Koettlitz
 * <br>Erstellt am 14.07.2017
 */
public interface ChannelListener<T> {
   /**
    * Handle the message that was received through the channel.
    *
    * @param msg The received message
    */
   public void received(T msg);

   public static class ChannelListenerChain<T> extends LinkedList<ChannelListener<T>>
                                                       implements ChannelListener<T> {
      private static final long serialVersionUID = 1L;

      @Override
      public void received(T packet) {
         @SuppressWarnings("unchecked")
         ChannelListener<T>[] listeners = toArray(new ChannelListener[size()]);
         for (ChannelListener<T> l : listeners)
            l.received(packet);
      }
   }
}