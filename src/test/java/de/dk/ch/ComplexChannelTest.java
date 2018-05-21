package de.dk.ch;

import static de.dk.ch.TestObject.DEFAULT_MSG;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * 11.05.2018
 */
public class ComplexChannelTest {
   private Multiplexer multiplexerA;
   private Multiplexer multiplexerB;

   private ChannelPair<String> pair0;
   private ChannelPair<String> pair1;
   private ChannelPair<TestObject> pair2;

   public ComplexChannelTest() {

   }

   @BeforeEach
   public void init() {
      IDGenerator idGen = new SimpleIterativeIdGenerator();
      TestChannelHandler<String> stringChannelHandlerA = new TestChannelHandler<>(String.class);
      TestChannelHandler<TestObject> messageChannelHandlerA = new TestChannelHandler<>(TestObject.class);
      this.multiplexerA = new Multiplexer(idGen, m -> multiplexerB.receive(m), stringChannelHandlerA, messageChannelHandlerA);

      TestChannelHandler<String> stringChannelHandlerB = new TestChannelHandler<>(String.class);
      TestChannelHandler<TestObject> messageChannelHandlerB = new TestChannelHandler<>(TestObject.class);
      this.multiplexerB = new Multiplexer(idGen, m -> multiplexerA.receive(m), stringChannelHandlerB, messageChannelHandlerB);

      this.pair0 = new ChannelPair<>(String.class, multiplexerA, stringChannelHandlerA, stringChannelHandlerB);
      this.pair1 = new ChannelPair<>(String.class, multiplexerA, stringChannelHandlerA, stringChannelHandlerB);
      this.pair2 = new ChannelPair<>(TestObject.class, multiplexerA, messageChannelHandlerA, messageChannelHandlerB);
   }

   @Test
   public void messagesAreRedirectedToTheCorrectChannels() {
      pair0.msgAToB(DEFAULT_MSG);
      pair1.msgBToA(DEFAULT_MSG);
      pair2.msgAToB(TestObject.defaultMessage());
   }

   @Test
   public void closeOfOneChannelTriggersTheCorrespondingOneToClose() {
      pair0.closeA();
      pair1.closeB();
      pair2.closeA();
   }

   @Test
   public void closingAChannelDoesNotAffectOtherChannels() {
      pair0.closeA();
      pair1.msgAToB(DEFAULT_MSG);
      pair2.msgBToA(TestObject.defaultMessage());
   }

   @Test
   public void closingOfMultiplexerClosesAllChannels() {
      multiplexerA.close();
      assertTrue(multiplexerA.isClosed(), "Multiplexer not closed after call of close()");
      assertTrue(pair0.getChannelA().isClosed(), "Channel not closed after multiplexer closed.");
      assertTrue(pair0.getChannelB().isClosed(), "Channel not closed after multiplexer closed.");
      assertTrue(pair1.getChannelA().isClosed(), "Channel not closed after multiplexer closed.");
      assertTrue(pair1.getChannelB().isClosed(), "Channel not closed after multiplexer closed.");
      assertTrue(pair2.getChannelA().isClosed(), "Channel not closed after multiplexer closed.");
      assertTrue(pair2.getChannelB().isClosed(), "Channel not closed after multiplexer closed.");
   }

   @AfterEach
   public void cleanUp() {
      multiplexerA.close();
      multiplexerB.close();
   }

}
