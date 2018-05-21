package de.dk.ch;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeoutException;

import de.dk.ch.ChannelPacket.ChannelPacketType;

class NewChannelRequest<P> implements Callable<Channel<P>> {
   private final Channel<P> channel;
   private final Class<P> type;
   private final P initialMessage;
   private ChannelRefusedPacket refuseResponse;
   private State state;

   protected NewChannelRequest(Channel<P> channel, Class<P> type, P initialMsg) {
      this.channel = channel;
      this.type = type;
      this.initialMessage = initialMsg;
   }

   protected NewChannelRequest(Channel<P> channel, Class<P> type) {
      this(channel, type, null);
   }

   @Override
   public synchronized Channel<P> call() throws InterruptedException,
                                                ChannelDeclinedException,
                                                IOException {
      try {
         return request(0);
      } catch (TimeoutException e) {
         throw new IllegalStateException();
      }
   }

   public synchronized Channel<P> request(long timeout) throws InterruptedException,
                                                               ChannelDeclinedException,
                                                               IOException,
                                                               TimeoutException {
      this.state = State.WAITING;
      channel.send(new NewChannelRequestPacket(channel.getId(), type, initialMessage));
      if (state == State.WAITING)
         wait(timeout);

      switch (state) {
      case ACCEPTED:
         channel.send(new ChannelPacket(channel.getId(), ChannelPacketType.OK));
         channel.setState(ChannelState.OPEN);
         return channel;
      case REFUSED:
         if (refuseResponse.getException() instanceof ChannelDeclinedException)
            throw (ChannelDeclinedException) refuseResponse.getException();
         else
            throw new IOException(refuseResponse.getException());
      case WAITING:
         throw new TimeoutException("The channel request timed out.");
      }

      return channel;
   }

   public synchronized void refused(ChannelRefusedPacket response) {
      this.refuseResponse = response;
      this.state = State.REFUSED;
      notify();
   }

   public synchronized void accepted() {
      this.state = State.ACCEPTED;
      notify();
   }

   protected Channel<?> getChannel() {
      return channel;
   }

   protected Class<P> getType() {
      return type;
   }

   private static enum State {
      WAITING,
      ACCEPTED,
      REFUSED;
   }
}