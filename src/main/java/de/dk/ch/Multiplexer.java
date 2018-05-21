package de.dk.ch;

import static de.dk.util.CollectionUtils.toArray;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dk.ch.ChannelPacket.ChannelPacketType;

/**
 * The <code>Multiplexer</code> class is the core of the multiplexing API.
 * A <code>Multiplexer</code> manages all the established channels,
 * the opening and closing procedures of a channel through the attached {@link ChannelHandler}s and
 * redirects the received messages to the channels.
 *
 * A <code>Multiplexer</code> needs <code>ChannelHandler</code>s to establish new channels.
 * The <code>ChannelHandler</code>s are instances to handle requests to open a channel.
 *
 * @author David Koettlitz
 * <br>Erstellt am 14.07.2017
 *
 * @see ChannelHandler
 * @see Channel
 * @see ChannelListener
 */
public class Multiplexer implements Receiver {
   private static final Logger log = LoggerFactory.getLogger(Multiplexer.class);

   private static final String RECEIVE_METHOD_NAME = "receive";
   private static final String NEW_CHANNEL_METHOD_NAME = "newChannelRequested";
   private static final String CLOSED_METHOD = "channelClosed";

   private final IDGenerator idGenerator;
   private final Sender sender;
   private final Map<Long, Channel<?>> channels = new ConcurrentHashMap<>();
   private final Map<Class<?>, ChannelHandler<?>> handlers = new ConcurrentHashMap<>();
   private final Map<Long, ChannelHandler<?>> channelAssociatedHandlers = new ConcurrentHashMap<>();
   private final Map<Long, NewChannelRequest<?>> requests = new ConcurrentHashMap<>();

   private boolean closed = false;

   /**
    * Creates a new multiplexer.
    *
    * @param idGenerator The id generator to generate the ids of the channels
    * @param sender The sender for the channels to send messages with
    * @param handlers The channel handlers to handle new channel requests and closing of channels
    * (channel handlers can also be added later using the {@link Multiplexer#addHandler(ChannelHandler)} method
    */
   public Multiplexer(IDGenerator idGenerator, Sender sender, ChannelHandler<?>... handlers) {
      this.idGenerator = idGenerator;
      this.sender = Objects.requireNonNull(sender);
      if (handlers != null) {
         for (ChannelHandler<?> handler : handlers)
            this.handlers.put(handler.getType(), handler);
      }
   }

   private static void invokeNewChannel(ChannelHandler<?> handler,
                                        Channel<?> channel,
                                        Object initialMsg) throws ChannelDeclinedException,
                                                                  IOException {
      try {
         Method receiveMethod = ChannelHandler.class.getDeclaredMethod(NEW_CHANNEL_METHOD_NAME, Channel.class, Optional.class);
         receiveMethod.invoke(handler, channel, initialMsg);
      } catch (NoSuchMethodException
               | SecurityException
               | IllegalAccessException
               | IllegalArgumentException e) {
         throw new IOException("Could not invoke newChannelRequest method of the channel handler.", e);

      } catch (InvocationTargetException e) {
         if (e.getTargetException() instanceof ChannelDeclinedException)
            throw (ChannelDeclinedException) e.getTargetException();
         else {
            log.error("The handler of channel with id " + channel.getId() + " threw an exception.", e);
            throw new IOException("The channel handler threw an exception handling the NewChannelRequestPacket", e);
         }
      }
   }

   private static void invokeClosed(ChannelHandler<?> handler, Channel<?> channel) {
      try {
         Method receiveMethod = ChannelHandler.class.getDeclaredMethod(CLOSED_METHOD, Channel.class);
         receiveMethod.invoke(handler, channel);
      } catch (NoSuchMethodException
               | SecurityException
               | IllegalAccessException
               | IllegalArgumentException e) {
         log.error("Could not invoke receive method of channel!", e);
      } catch (InvocationTargetException e) {
         log.error("The handler of channel with id " + channel.getId() + " threw an exception.", e);
      }
   }

   private static void redirectPacket(Channel<?> channel, Packet packet) {
      try {
         Method receiveMethod = Channel.class.getDeclaredMethod(RECEIVE_METHOD_NAME, PayloadPacket.class);
         receiveMethod.invoke(channel, packet);
      } catch (NoSuchMethodException
               | SecurityException
               | IllegalAccessException
               | IllegalArgumentException e) {
         log.error("Could not invoke receive method of channel!", e);
      } catch (InvocationTargetException e) {
         log.error("The receiver of channel with the id " + channel.getId() + " threw an exception.", e);
      }
   }

   /**
    * Establishes a new channel to communicate through.
    * The "other side" will receive a request and the <code>ChannelHandler</code> of the matching <code>type</code>
    * will receive the request, by the {@link ChannelHandler#newChannelRequested(Channel, Optional)} method getting called.
    *
    * @param type The type of the new channel
    * @param timeout The timeout in milliseconds for the request
    * @param initialMsg An optional initial message to send with the request
    * @param <T> The type of the messages that go through the channel
    *
    * @return The new established channel.
    * The channel will be in <code>OPEN</code> state and ready for communication.
    *
    * @throws IOException If an I/O error occurs while establishing a new channel
    * @throws ClosedException if this multiplexer has already been closed
    * @throws ChannelDeclinedException If the "other side" refuses to open the channel
    * @throws InterruptedException If the thread is interrupted while waiting for the channel to be established
    * @throws TimeoutException If the given <code>timeout</code> is reached before a new channel could be established
    */
   public <T> Channel<T> establishNewChannel(Class<T> type,
                                             long timeout,
                                             T initialMsg) throws IOException,
                                                                  ClosedException,
                                                                  ChannelDeclinedException,
                                                                  InterruptedException,
                                                                  TimeoutException {
      ensureOpen();
      NewChannelRequest<T> request = createRequest(type, initialMsg);
      return request.request(timeout);
   }

   /**
    * Establishes a new channel to communicate through.
    * The "other side" will receive a request and the <code>ChannelHandler</code> of the matching <code>type</code>
    * will receive the request, by the {@link ChannelHandler#newChannelRequested(Channel, Optional)} method getting called.
    *
    * @param type The type of the new channel
    * @param timeout The timeout in milliseconds for the request
    * @param <T> The type of the messages that go through the channel
    *
    * @return The new established channel.
    * The channel will be in <code>OPEN</code> state and ready for communication.
    *
    * @throws IOException If an I/O error occurs while establishing a new channel
    * @throws ClosedException if this multiplexer has already been closed
    * @throws ChannelDeclinedException If the "other side" refuses to open the channel
    * @throws InterruptedException If the thread is interrupted while waiting for the channel to be established
    * @throws TimeoutException If the given <code>timeout</code> is reached before a new channel could be established
    */
   public <T> Channel<T> establishNewChannel(Class<T> type,
                                             long timeout) throws IOException,
                                                                  ClosedException,
                                                                  ChannelDeclinedException,
                                                                  InterruptedException,
                                                                  TimeoutException {
      return establishNewChannel(type, timeout, null);
   }

   /**
    * Asynchronously establishes a new channel in a background thread.
    * The "other side" will receive a request and the <code>ChannelHandler</code> of the matching <code>type</code>
    * will receive the request, by the {@link ChannelHandler#newChannelRequested(Channel, Optional)} method getting called.
    *
    * @param type The type of the new channel
    * @param initialMsg An optional initial message to send with the request
    * @param <T> The type of the messages that go through the channel
    *
    * @return A future to represent the establishment of the new channel.
    *
    * @throws ClosedException if this multiplexer has already been closed.
    */
   public <T> Future<Channel<T>> asynchEstablishNewChannel(Class<T> type, T initialMsg) throws ClosedException {
      ensureOpen();
      NewChannelRequest<T> request = createRequest(type, initialMsg);
      FutureTask<Channel<T>> task = new FutureTask<>(request);
      new Thread(task).start();
      return task;
   }

   /**
    * Asynchronously establishes a new channel in a background thread.
    * The "other side" will receive a request and the <code>ChannelHandler</code> of the matching <code>type</code>
    * will receive the request, by the {@link ChannelHandler#newChannelRequested(Channel, Optional)} method getting called.
    *
    * @param type The type of the new channel
    * @param <T> The type of the messages that go through the channel
    *
    * @return A future to represent the establishment of the new channel.
    * @throws ClosedException if this multiplexer has already been closed.
    */
   public <T> Future<Channel<T>> asynchEstablishNewChannel(Class<T> type) throws ClosedException {
      return asynchEstablishNewChannel(type, null);
   }

   private <T> NewChannelRequest<T> createRequest(Class<T> type, T initialMsg) {
      long id = idGenerator.nextId();
      Channel<T> channel = new Channel<>(id, sender, this);
      NewChannelRequest<T> request = new NewChannelRequest<>(channel, type, initialMsg);
      requests.put(id, request);
      return request;
   }

   @Override
   public void receive(Object object) throws IllegalArgumentException, IllegalStateException {
      if (closed)
         throw new IllegalStateException("Multiplexer has already been closed.");

      if (!(object instanceof Packet))
         throw new IllegalArgumentException("An object was received, that was not a packet: " + object);

      Packet packet = (Packet) object;

      if (packet instanceof ChannelPacket) {
         handleChannelPacket((ChannelPacket) packet);
         return;
      }

      Channel<?> channel = channels.get(packet.channelId);
      if (channel == null)
         throw new IllegalArgumentException("No channel established for packet: " + packet);

      if (channel.isClosed())
         log.warn("Packet for closed channel with id " + channel.getId() + " received.");
      else
         redirectPacket(channel, packet);
   }

   private void handleChannelPacket(ChannelPacket packet) {
      switch (packet.getPacketType()) {
      case NEW:
         newChannelRequest((NewChannelRequestPacket) packet);
         break;
      case OK:
         channelAccepted(packet.channelId);
         break;
      case REFUSED:
         channelRefused((ChannelRefusedPacket) packet);
         break;
      case CLOSE:
         Channel<?> channel = channels.get(packet.channelId);
         if (channel == null) {
            log.warn("ChannelClosedPacket with id " + packet.channelId
                        + " received, but no channel with that id was found.");
            break;
         }
         channelClosed(channel);
         break;
      }
   }

   private void channelAccepted(long channelId) {
      NewChannelRequest<?> request = requests.remove(channelId);
      if (request != null) {
         addChannel(request.getChannel(), getHandlerFor(request.getType()));
         channels.put(request.getChannel().getId(), request.getChannel());
         request.accepted();
      } else {
         Channel<?> channel = channels.get(channelId);
         if (channel == null) {
            log.warn("Could not handle channelPacket with ChannelPacketType OK and channelId: " + channelId);
         } else {
            try {
               channel.setState(ChannelState.OPEN);
            } catch (ClosedException e) {
               // Nothing to do here
            }
         }
      }

   }

   private void channelRefused(ChannelRefusedPacket packet) {
      NewChannelRequest<?> request = requests.remove(packet.channelId);
      if (request == null)
         log.warn("No channel request for id: " + packet.channelId + " registered.");
      else
         request.refused(packet);
   }

   protected synchronized void channelClosed(Channel<?> channel) {
      try {
         channel.setState(ChannelState.CLOSED);
      } catch (ClosedException e) {
         // Nothing to do here
      }
      channels.remove(channel.getId());
      ChannelHandler<?> handler = channelAssociatedHandlers.get(channel.getId());
      if (handler != null)
         invokeClosed(handler, channel);
   }

   private void newChannelRequest(NewChannelRequestPacket request) {
      ChannelPacket response;
      Class<?> packetType = request.getType();
      log.debug("NewChannelRequestPacket for PacketType: " + packetType.getName() + " received.");
      ChannelHandler<?> handler = getHandlerFor(packetType);
      if (handler == null) {
         String msg = "No ChannelHandler registered for PacketType: " + packetType;
         log.info(msg);
         log.info("Refusing NewChannelRequestPacket");
         response = new ChannelRefusedPacket(request.channelId, msg);
      } else {
         Channel<?> channel = new Channel<>(request.channelId, sender, this);
         try {
            invokeNewChannel(handler, channel, request.getInitialMessage());
            log.debug("Accepting new channel request");
            response = new ChannelPacket(channel.getId(), ChannelPacketType.OK);
            addChannel(channel, handler);
         } catch (ChannelDeclinedException | IOException e) {
            log.debug("Refusing new channel request", e);
            response = new ChannelRefusedPacket(request.channelId, e);
         }
      }

      try {
         sender.send(response);
      } catch (IOException e) {
         log.warn("Could not send refuse for NewChannelRequestPacket", e);
         return;
      }
   }

   private void ensureOpen() throws ClosedException {
      if (closed)
         throw new ClosedException("Multiplexer has already been closed.");
   }

   private ChannelHandler<?> getHandlerFor(Class<?> type) {
      if (type == null)
         return null;

      ChannelHandler<?> handler = handlers.get(type);
      if (handler != null)
         return handler;

      return getHandlerFor((Class<?>) type.getSuperclass());
   }

   protected void addChannel(Channel<?> channel, ChannelHandler<?> handler) {
      channels.put(channel.getId(), channel);
      if (handler != null)
         channelAssociatedHandlers.put(channel.getId(), handler);
   }

   /**
    * Get the channel with the given <code>id</code>.
    *
    * @param id The id of the channel
    *
    * @return The channel with the given <code>id</code> if present -
    * <code>null</code> otherwise
    */
   public synchronized Channel<?> getChannel(long id) {
      return channels.get(id);
   }

   /**
    * Add a channel handler to be called at incoming requests to open new channels
    * matching the <code>handler</code>s type.<br>
    * If a handler with the <code>handlers</code> type is already registered, it will be overwritten with this handler.<br>
    * Incoming requests for new channels with a subtype of the type of this handler the handler will also be called
    * if no other handler matching the type is registered.<br>
    * I other words: if a channel request arrives the channel manager will look for a handler matching exactly the type.
    * If no such handler is registered a handler of the next supertype will be looked for. If no such handler is found
    * the next supertype handler will be looked for and so on...
    *
    * @param handler The handler to handle channel requests of the handlers type
    */
   public void addHandler(ChannelHandler<?> handler) {
      handlers.put(handler.getType(), handler);
   }

   /**
    * Removes a handler from this channel manager.
    *
    * @param handler The handler to be removed
    */
   public void removeHandler(ChannelHandler<?> handler) {
      removeHandler(handler.getType());
   }

   /**
    * Removes a handler from this channel manager by its type.
    *
    * @param type The type of the handler to be removed.
    */
   public void removeHandler(Class<?> type) {
      handlers.remove(type);
   }

   /**
    * Get the sender via which the channels established by this channel manager are sending their messages.
    *
    * @return The sender
    */
   public Sender getSender() {
      return sender;
   }

   /**
    * Closes this multiplexer and all of its channels.
    */
   public synchronized void close() {
      closed = true;
      Channel<?>[] channels = toArray(this.channels.values(), Channel<?>[]::new);
      for (Channel<?> channel : channels) {
         try {
            channel.close();
         } catch (IOException e) {
            // Nothing to do here
         }
      }
   }

   public boolean isClosed() {
      return closed;
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((this.handlers == null) ? 0 : this.handlers.hashCode());
      result = prime * result + ((this.idGenerator == null) ? 0 : this.idGenerator.hashCode());
      return result;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj)
         return true;
      if (obj == null)
         return false;
      if (getClass() != obj.getClass())
         return false;
      Multiplexer other = (Multiplexer) obj;
      if (this.handlers == null) {
         if (other.handlers != null)
            return false;
      } else if (!this.handlers.equals(other.handlers))
         return false;
      if (this.idGenerator == null) {
         if (other.idGenerator != null)
            return false;
      } else if (!this.idGenerator.equals(other.idGenerator))
         return false;
      return true;
   }

   @Override
   public String toString() {
      StringBuilder builder = new StringBuilder("Multiplexer {\n");
      builder.append("\tchannelcount=")
             .append(channels.size())
             .append(",\n\thandlerTypes=[");

      Iterator<Class<?>> iterator = handlers.keySet().iterator();
      for (Class<?> type = iterator.next(); iterator.hasNext(); type = iterator.next()) {
         builder.append("\t\t")
                .append(type.getName())
                .append(iterator.hasNext() ? ",\n" : "\n\t]");
      }

      return builder.append("\n}")
                    .toString();
   }
}