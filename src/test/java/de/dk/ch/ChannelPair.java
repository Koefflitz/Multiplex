package de.dk.ch;

import static de.dk.ch.SimpleChannelTest.TIMEOUT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * 11.05.2018
 */
public class ChannelPair<T> {
   private final Channel<T> channelA;
   private final Channel<T> channelB;

   private final ChannelHandler<T> handlerA;
   private final ChannelHandler<T> handlerB;

   private final TestChannelListener<T> listenerA;
   private final TestChannelListener<T> listenerB;

   public ChannelPair(Class<T> msgType, Multiplexer multiplexer, TestChannelHandler<T> handlerA, TestChannelHandler<T> handlerB) {
      this.handlerA = handlerA;
      this.handlerB = handlerB;
      try {
         this.channelA = multiplexer.establishNewChannel(msgType, TIMEOUT);
      } catch (IOException | ChannelDeclinedException | InterruptedException | TimeoutException e) {
         fail("Could not establish channel.", e);
         throw new Error("This error will never be thrown.");
      }

      this.channelB = handlerB.getChannel(channelA.getId());
      assertNotNull(channelB, "After establishing a channel the channel of the other end was not present.");

      channelA.addListener(this.listenerA = new TestChannelListener<>());
      channelB.addListener(this.listenerB = new TestChannelListener<>());
   }

   public void msgAToB(T msg) {
      sendMessage(msg, channelA, listenerB);
   }

   public void msgBToA(T msg) {
      sendMessage(msg, channelB, listenerA);
   }

   private void sendMessage(T msg, Channel<T> sender, TestChannelListener<T> receiver) {
      try {
         sender.send(msg);
      } catch (IOException e) {
         fail("Could not send message through channel.", e);
      }

      T received;
      try {
         received = receiver.waitGetAndThrowAwayPacket(TIMEOUT);
      } catch (InterruptedException e) {
         fail("Interrupting while receiving message.", e);
         return;
      }
      assertEquals(msg, received);
   }

   public void closeA() {
      close(channelA, channelB);
   }

   public void closeB() {
      close(channelB, channelA);
   }

   private void close(Channel<T> trigger, Channel<T> reactor) {
      try {
         trigger.close();
      } catch (IOException e) {
         fail("Failed to close channel.", e);
      }

      assertTrue(reactor.isClosed(), "After closing a channel, the corresponding channel was not closed.");
   }

   public Channel<T> getChannelA() {
      return channelA;
   }

   public Channel<T> getChannelB() {
      return channelB;
   }

   public ChannelHandler<T> getHandlerA() {
      return handlerA;
   }

   public ChannelHandler<T> getHandlerB() {
      return handlerB;
   }

   public TestChannelListener<T> getListenerA() {
      return listenerA;
   }

   public TestChannelListener<T> getListenerB() {
      return listenerB;
   }

}
