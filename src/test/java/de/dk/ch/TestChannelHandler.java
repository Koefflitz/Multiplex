package de.dk.ch;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import de.dk.ch.Channel;
import de.dk.ch.ChannelDeclinedException;
import de.dk.ch.ChannelHandler;

/**
 * @author David Koettlitz
 * <br>Erstellt am 07.08.2017
 */
public class TestChannelHandler<P> implements ChannelHandler<P> {
   private final Class<P> packetType;
   private final Map<Long, Channel<P>> channels = new HashMap<>();

   private boolean accept = true;

   public TestChannelHandler(Class<P> packetType) {
      this.packetType = packetType;
   }

   @Override
   public void newChannelRequested(Channel<P> channel, Optional<P> initialMessage) throws ChannelDeclinedException {
      if (!accept)
         throw new ChannelDeclinedException("Not accepting channels right now");

      channels.put(channel.getId(), channel);
   }

   @Override
   public void channelClosed(Channel<P> channel) {
      channels.remove(channel.getId());
      synchronized (channel) {
         channel.notify();
      }
   }

   public synchronized void waitForClose(Channel<P> channel, long timeout) throws InterruptedException {
      synchronized (channel) {
         if (channel.isClosed())
            return;

         channel.wait(timeout);
      }
   }

   @Override
   public Class<P> getType() {
      return packetType;
   }

   public Channel<P> getChannel(long id) {
      return channels.get(id);
   }

   public boolean isAcceptingChannelRequests() {
      return accept;
   }

   public void setAcceptChannelRequests(boolean accept) {
      this.accept = accept;
   }

   public void add(Channel<P> channel) {
      channels.put(channel.getId(), channel);
   }
}
