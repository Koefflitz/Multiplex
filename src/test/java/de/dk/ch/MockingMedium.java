package de.dk.ch;

import java.io.Serializable;
import java.util.function.Supplier;

public class MockingMedium implements Sender {
   public static final int MIN_DELAY_MILLIS = 16;
   public static final int MAX_DELAY_MILLIS = 128;

   private Supplier<Multiplexer> target;

   public MockingMedium(Supplier<Multiplexer> target) {
      this.target = target;
   }

   @Override
   public void send(Serializable msg) throws IllegalArgumentException {
      int sleepTime = Math.max(MIN_DELAY_MILLIS, (int) (Math.random() * MAX_DELAY_MILLIS));

      try {
         Thread.sleep(sleepTime);
      } catch (InterruptedException e) {
         throw new IllegalStateException("Interrupted while mocking a delay for the message delivery", e);
      }

      target.get()
            .receive(msg);
   }

}
