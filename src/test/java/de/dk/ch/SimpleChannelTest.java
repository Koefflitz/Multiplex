package de.dk.ch;

import static de.dk.ch.TestObject.DEFAULT_MSG;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 11.05.2018
 */
public class SimpleChannelTest {
   private static final Logger log = LoggerFactory.getLogger(SimpleChannelTest.class);

   public static final long TIMEOUT = 128;

   private Multiplexer multiplexerA;
   private Multiplexer multiplexerB;

   private TestChannelHandler<String> handlerA;
   private TestChannelHandler<String> handlerB;

   private Channel<String> channelA;
   private Channel<String> channelB;

   private TestChannelListener<String> listenerA;
   private TestChannelListener<String> listenerB;

   public SimpleChannelTest() {

   }

   @BeforeEach
   public void init() {
      IDGenerator idGen = new SimpleIterativeIdGenerator();
      this.handlerA = new TestChannelHandler<>(String.class);
      this.multiplexerA = new Multiplexer(idGen, m -> multiplexerB.receive(m), handlerA);

      this.handlerB = new TestChannelHandler<>(String.class);
      this.multiplexerB = new Multiplexer(idGen, m -> multiplexerA.receive(m), handlerB);
   }

   @Test
   public void channelEstablishes() {
      try {
         channelA = multiplexerA.establishNewChannel(String.class, TIMEOUT);
      } catch (IOException | ChannelDeclinedException | InterruptedException | TimeoutException e) {
         fail("Could not establish channel.", e);
      }

      channelB = handlerB.getChannel(channelA.getId());
      assertNotNull(channelB, "Channel on other side not established.");

      assertFalse(channelA.isClosed(), "Channel was closed right after establishment.");
      assertFalse(channelB.isClosed(), "Channel was closed right after establishment.");

      channelA.addListener((listenerA = new TestChannelListener<>()));
      channelB.addListener((listenerB = new TestChannelListener<>()));
   }

   private static <T> void messageGoesThrough(Channel<T> sender, TestChannelListener<T> receiver, T msg) {
      try {
         sender.send(msg);
      } catch (IOException e) {
         fail("Failed to send message.", e);
      }

      T received = null;
      try {
         received = receiver.waitGetAndThrowAwayPacket(TIMEOUT);
      } catch (InterruptedException e) {
         fail("Interrupted while receiving the message.", e);
      }
      assertEquals(msg, received);
   }

   @Test
   public void singleMessageGoesThrough() {
      channelEstablishes();
      messageGoesThrough(channelA, listenerB, DEFAULT_MSG);
   }

   @Test
   public void multipleMessagesGoThroughBothDirections() {
      channelEstablishes();
      messageGoesThrough(channelA, listenerB, DEFAULT_MSG);
      messageGoesThrough(channelA, listenerB, DEFAULT_MSG);
      messageGoesThrough(channelB, listenerA, DEFAULT_MSG);
      messageGoesThrough(channelB, listenerA, DEFAULT_MSG);
   }

   @Test
   public void correspondingChannelClosesAfterClosingTheOther() {
      channelEstablishes();
      try {
         channelA.close();
      } catch (IOException e) {
         fail("Failed to close channel.", e);
      }
      assertTrue(channelB.isClosed(), "After closing a channel, the corresponding channel is still open.");
   }

   @Test
   public void cannotSendMessageThroughClosedChannel() {
      correspondingChannelClosesAfterClosingTheOther();
      assertThrows(ClosedException.class, () -> channelA.send(DEFAULT_MSG));
      assertThrows(ClosedException.class, () -> channelB.send(DEFAULT_MSG));
   }

   @AfterEach
   public void cleanUp() {
      try {
         if (channelA != null && !channelA.isClosed())
               channelA.close();

         if (channelB != null && !channelB.isClosed())
            channelB.close();

      } catch (IOException e) {
         log.warn("Error closing the channels at the end.", e);
      }
      multiplexerA.close();
   }

}
