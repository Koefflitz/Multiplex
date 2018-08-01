package de.dk.ch;

import java.util.function.Supplier;

public class AsyncChannelTest extends ComplexChannelTest {

   public AsyncChannelTest() {

   }

   @Override
   protected Sender createMediumInFrontOf(Supplier<Multiplexer> target) {
      return new MockingMedium(target);
   }

   @Override
   protected <T> ChannelPair<T> createChannelPair(Class<T> msgType,
                                                  Multiplexer multiplexer,
                                                  TestChannelHandler<T> handlerA,
                                                  TestChannelHandler<T> handlerB) {

      return new AsyncChannelPair<>(msgType, multiplexer, handlerA, handlerB);
   }

}
