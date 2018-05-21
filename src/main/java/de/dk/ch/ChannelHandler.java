package de.dk.ch;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import de.dk.util.function.UnsafeBiConsumer;


/**
 * An instance for handling requests for new channels and closes of channels of a specific type.
 * A <code>ChannelHandler</code> can be registered at a {@link Multiplexer}
 * and will be informed whenever a request to open a channel of the type that matches this <code>ChannelHandler</code>
 * is received or when such a <code>Channel</code> is closed.
 *
 * @param <T> The type of the channels
 *
 * @author David Koettlitz
 * <br>Erstellt am 14.07.2017
 *
 * @see Multiplexer
 */
public interface ChannelHandler<T> {
   /**
    * This method is called whenever a request to open a new channel of the matching type is requested.
    * To decline the request a <code>ChannelDeclinedException</code> is thrown.
    * Otherwise the channel will be opened to be ready for communication.
    *
    * @param channel The channel that is requested to be opened
    * @param initialMessage An optional initial message that came with the request.
    *
    * @throws ChannelDeclinedException If the channel should not be opened
    */
   public void newChannelRequested(Channel<T> channel, Optional<T> initialMessage) throws ChannelDeclinedException;

   /**
    * This method is called when a channel has been closed.
    *
    * @param channel The closed channel
    */
   public void channelClosed(Channel<T> channel);

   /**
    * Get the type this channel handler can handle.
    *
    * @return The type this channel handler can handle
    */
   public Class<T> getType();

   /**
    * Create a new <code>ChannelHandler</code> that consists of the given operations.
    *
    * @param <T> The type of the ChannelHandler
    * @param type The type of the ChannelHandler
    * @param openHandler The implementation for the
    *    {@link #newChannelRequested(Channel, Optional)} method
    * @param closeHandler The implementation for the
    *    {@link #channelClosed(Channel)} method
    *
    * @return A new <code>ChannelHandler</code>, that consists of the given operations
    *
    * @throws NullPointerException if <code>type</code> is <code>null</code>
    */
   public static <T> ChannelHandler<T> of(Class<T> type,
                                          UnsafeBiConsumer<Channel<T>,
                                                           Optional<T>,
                                                           ChannelDeclinedException> openHandler,
                                          Consumer<Channel<T>> closeHandler) throws NullPointerException {
      return new Adapter<>(type, openHandler, closeHandler);
   }

   /**
    * Create a new <code>ChannelHandler</code> that consists of the given operations.
    * The channels handlers {@link #channelClosed(Channel)} method will be empty.
    *
    * @param <T> The type of the ChannelHandler
    * @param type The type of the ChannelHandler
    * @param openHandler The implementation for the
    *    {@link #newChannelRequested(Channel, Optional)} method
    *
    * @return A new <code>ChannelHandler</code>, that consists of the given operations
    *
    * @throws NullPointerException if <code>type</code> is <code>null</code>
    */
   public static <T> ChannelHandler<T> of(Class<T> type,
                                          UnsafeBiConsumer<Channel<T>,
                                                           Optional<T>,
                                                           ChannelDeclinedException> openHandler) throws NullPointerException {
      return new Adapter<>(type, openHandler, null);
   }

   static class Adapter<T> implements ChannelHandler<T> {
      private Class<T> type;
      private UnsafeBiConsumer<Channel<T>, Optional<T>, ChannelDeclinedException> openHandler;
      private Consumer<Channel<T>> closeHandler;

      private Adapter(Class<T> type,
                     UnsafeBiConsumer<Channel<T>,
                                      Optional<T>,
                                      ChannelDeclinedException> openHandler,
                     Consumer<Channel<T>> closeHandler) throws NullPointerException {

         this.type = Objects.requireNonNull(type);
         this.openHandler = Objects.requireNonNull(openHandler);
         this.closeHandler = closeHandler;
      }

      @Override
      public void newChannelRequested(Channel<T> channel,
                                      Optional<T> initialMessage) throws ChannelDeclinedException {
         openHandler.accept(channel, initialMessage);
      }

      @Override
      public void channelClosed(Channel<T> channel) {
         if (closeHandler != null)
            closeHandler.accept(channel);
      }

      @Override
      public Class<T> getType() {
         return type;
      }

   }
}