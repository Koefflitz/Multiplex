package de.dk.ch;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeoutException;

import de.dk.ch.de.dk.ch.ex.ChannelDeclinedException;

class NewChannelRequest implements Callable<Channel> {
   private Multiplexer multiplexer;
   private final Channel channel;
   private final byte[] initialMessage;
   private String refuseResponse;
   private State state;

   NewChannelRequest(Channel channel, byte[] initialMsg) {
      this.channel = channel;
      this.initialMessage = initialMsg;
   }

   NewChannelRequest(Channel channel) {
      this(channel, null);
   }

   @Override
   public synchronized Channel call() throws InterruptedException,
           ChannelDeclinedException,
                                                IOException {
      try {
         return request(0);
      } catch (TimeoutException e) {
         throw new IllegalStateException();
      }
   }

   synchronized Channel request(long timeout) throws InterruptedException,
                                                            ChannelDeclinedException,
                                                            IOException,
                                                            TimeoutException {
      this.state = State.WAITING;
      if (initialMessage != null)
         multiplexer.send(channel.getId(), MessageType.NEW, initialMessage);
      else
         multiplexer.send(channel.getId(), MessageType.NEW);

      if (state == State.WAITING)
         wait(timeout);

      switch (state) {
      case ACCEPTED:
         channel.setState(ChannelState.OPEN);
         return channel;
      case REFUSED:
         if (refuseResponse != null)
            throw new ChannelDeclinedException(refuseResponse);
         else
            throw new ChannelDeclinedException();
      case WAITING:
         throw new TimeoutException("The channel request timed out.");
      }

      // Can technically never be reached, but to satisfy the compiler...
      throw new Error("Somethings really wrong here! Missed a case?");
   }

   synchronized void refused(String message) {
      this.refuseResponse = message;
      this.state = State.REFUSED;
      notify();
   }

   synchronized void accepted() {
      this.state = State.ACCEPTED;
      notify();
   }

   protected Channel getChannel() {
      return channel;
   }

   private enum State {
      WAITING,
      ACCEPTED,
      REFUSED
   }
}