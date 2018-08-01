package de.dk.ch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;

public class AsyncChannelPair<T> extends ChannelPair<T> {

   public AsyncChannelPair(Class<T> msgType,
                           Multiplexer multiplexer,
                           TestChannelHandler<T> handlerA,
                           TestChannelHandler<T> handlerB) {

      super(msgType, multiplexer, handlerA, handlerB);
   }

   @Override
   protected void sendMessage(T msg, Channel<T> sender, TestChannelListener<T> receiver) {
      Thread thread = new Thread(() -> {
         try {
            sender.send(msg);
         } catch (IOException e) {
            fail("Could not send message through channel.", e);
         }
      });
      thread.start();

      T received;
      try {
         received = receiver.waitGetAndThrowAwayPacket(MockingMedium.MAX_DELAY_MILLIS + 128);
      } catch (InterruptedException e) {
         fail("Interrupting while receiving message.", e);
         return;
      }
      assertEquals(msg, received);
   }

}
