package de.dk.ch;

import static de.dk.util.CollectionUtils.toArray;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeoutException;

import de.dk.ch.de.dk.ch.ex.ChannelDeclinedException;
import de.dk.ch.de.dk.ch.ex.ClosedException;
import de.dk.ch.de.dk.ch.ex.InvalidDataException;

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
public class Multiplexer {
   // FIXME remove
   private static final Logger log = new Logger();

   private static final int META_DATA_BASE_LENGTH = 2;
   private static final int META_DATA_LENGTH_LENGTH = Integer.SIZE / 8;

   private static final String RECEIVE_METHOD_NAME = "receive";
   private static final String NEW_CHANNEL_METHOD_NAME = "newChannelRequested";
   private static final String CLOSED_METHOD = "channelClosed";

   private final InputStream in;
   private final OutputStream out;

   private final Charset encoding = Charset.defaultCharset();

   private final IDGenerator idGenerator;
   private final Map<Byte, Channel> channels = new ConcurrentHashMap<>();
   private ChannelHandler channelHandler;
   private final Map<Byte, NewChannelRequest> requests = new ConcurrentHashMap<>();

   private boolean closed = false;

   /**
    * Creates a new multiplexer.
    *
    * @param idGenerator The id generator to generate the ids of the channels
    * @param in the InputStream to read data from
    * @param out the OutputStream to write data to
    * @param handler The channel handler to handle new channel requests and closing of channels
    */
   public Multiplexer(IDGenerator idGenerator,
                      InputStream in,
                      OutputStream out,
                      ChannelHandler handler) {

      this.idGenerator = idGenerator;
      this.in = Objects.requireNonNull(in);
      this.out = Objects.requireNonNull(out);
      this.channelHandler = handler;
   }

   static byte[] lengthToBytes(int length) {
      byte[] bytes = new byte[META_DATA_LENGTH_LENGTH];

      for (int i = 0; i < META_DATA_LENGTH_LENGTH; i++)
         bytes[i] = (byte) (length >> (i * 8));

      return bytes;
   }

   static int bytesToLength(byte[] wholeMessage, int offset) {
      int result = 0b0;

      for (int i = 0; i < META_DATA_LENGTH_LENGTH; i++) {
         int eightBit = wholeMessage[offset + i];
         result |= eightBit << (i * 8);
      }

      return result;
   }

   static byte[] serialize(Serializable object) {
      ByteArrayOutputStream buffer = new ByteArrayOutputStream();
      try {
         ObjectOutputStream serializer = new ObjectOutputStream(buffer);
         serializer.writeObject(object);
         serializer.close();
      } catch (IOException e) {
         // Does not happen
      }

      return buffer.toByteArray();
   }

   void send(byte channelId, MessageType type, byte[] data) throws IOException {
      send(channelId, type, data, 0, data.length);
   }

   void send(byte channelId, MessageType type, byte[] data, int offset, int length) throws IOException {
      byte[] lengthBytes = lengthToBytes(length);
      try {
         synchronized (out) {
            out.write(channelId);
            out.write(type.ordinal());
            out.write(lengthBytes);
            out.write(data, offset, length);
            out.flush();
         }
      } catch (IOException e) {
         // TODO really always close?
         close();
         throw e;
      }
   }

   void send(byte channelId, MessageType type) throws IOException {
      try {
         synchronized (out) {
            out.write(channelId);
            out.write(type.ordinal());
         }
      } catch (IOException e) {
         close();
         throw e;
      }
   }

   public void handle(byte[] data) throws InvalidDataException, ClosedException {
      if (closed)
         throw new ClosedException("Multiplexer has already been closed.");

      if (data == null || data.length < META_DATA_BASE_LENGTH)
         throw new InvalidDataException("Data was null or empty");

      byte channelId = data[0];
      int ordinal = data[1];
      MessageType type = MessageType.values()[ordinal];

      if (!type.hasPayload()) {
         handleMetaMessage(channelId, type);
         return;
      }

      if (data.length <= META_DATA_LENGTH_LENGTH)
         throw new InvalidDataException("Data for channel " + channelId + " was empty.");

      Channel channel = channels.get(channelId);
      if (channel == null)
         throw new InvalidDataException("Received message for channel with channelId " + channelId +
                                        ". But there was no channel established with that id.");

      if (channel.isClosed())
         throw new ClosedException("Received message for closed channel with id " + channelId);

      int length = bytesToLength(data, META_DATA_BASE_LENGTH);
      byte[] payload = new byte[length];
      int offset = META_DATA_BASE_LENGTH + META_DATA_LENGTH_LENGTH;
      System.arraycopy(data, offset, payload, 0, length);

      if (type == MessageType.DATA)
         channel.receive(payload);
      else // Must be MessageType.REFUSED
         channelRefused(channelId, new String(payload, encoding));
   }

   /**
    * Establishes a new channel to communicate through.
    * The "other side" will receive a request and the <code>ChannelHandler</code> of the matching <code>type</code>
    * will receive the request, by the {@link ChannelHandler#newChannelRequested(Channel, byte[])} method getting called.
    *
    * @param timeout The timeout in milliseconds for the request
    * @param initialMsg An optional initial message to send with the request
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
   public Channel establishNewChannel(long timeout,
                                      byte[] initialMsg) throws IOException,
                                                                ClosedException,
                                                                ChannelDeclinedException,
                                                                InterruptedException,
                                                                TimeoutException {
      ensureOpen();
      NewChannelRequest request = createRequest(initialMsg);
      return request.request(timeout);
   }

   /**
    * Establishes a new channel to communicate through.
    * The "other side" will receive a request and the <code>ChannelHandler</code> will receive
    * the request, by the {@link ChannelHandler#newChannelRequested(Channel, byte[])} method getting called.
    *
    * @param timeout The timeout in milliseconds for the request
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
   public Channel establishNewChannel(long timeout) throws IOException,
                                                           ClosedException,
                                                           ChannelDeclinedException,
                                                           InterruptedException,
                                                           TimeoutException {
      return establishNewChannel(timeout, null);
   }

   /**
    * Asynchronously establishes a new channel in a background thread.
    * The "other side" will receive a request and the <code>ChannelHandler</code> will receive
    * the request, by the {@link ChannelHandler#newChannelRequested(Channel, byte[])} method getting called.
    *
    * @param initialMsg An optional initial message to send with the request
    *
    * @return A future to represent the establishment of the new channel.
    *
    * @throws ClosedException if this multiplexer has already been closed.
    */
   public Future<Channel> asynchEstablishNewChannel(byte[] initialMsg) throws ClosedException {
      ensureOpen();
      NewChannelRequest request = createRequest(initialMsg);
      FutureTask<Channel> task = new FutureTask<>(request);
      new Thread(task).start();
      return task;
   }

   /**
    * Asynchronously establishes a new channel in a background thread.
    * The "other side" will receive a request and the <code>ChannelHandler</code> will receive
    * the request, by the {@link ChannelHandler#newChannelRequested(Channel, byte[])} method getting called.
    *
    * @return A future to represent the establishment of the new channel.
    * @throws ClosedException if this multiplexer has already been closed.
    */
   public Future<Channel> asynchEstablishNewChannel() throws ClosedException {
      return asynchEstablishNewChannel(null);
   }

   private NewChannelRequest createRequest(byte[] initialMsg) {
      byte id = idGenerator.nextId();
      Channel channel = new Channel(id, this);
      NewChannelRequest request = new NewChannelRequest(channel, initialMsg);
      requests.put(id, request);
      return request;
   }

   private void handleMetaMessage(byte channelId, MessageType type) {
      switch (type) {
      case NEW:
         newChannelRequest(channelId, null);
         break;
      case ACCEPT:
         channelAccepted(channelId);
         break;
      case CLOSE:
         Channel channel = channels.get(channelId);
         if (channel == null) {
            // strange message - maybe inform somebody?
            break;
         }
         channelClosed(channel);
         break;
      }
   }

   private void channelAccepted(byte channelId) {
      NewChannelRequest request = requests.remove(channelId);
      if (request != null) {
         channels.put(channelId, request.getChannel());
         request.accepted();
      } else {
         log.warn("Could not handle channelPacket with ChannelPacketType OK and channelId: " + channelId);
      }

   }

   private void channelRefused(byte channelId, String message) {
      NewChannelRequest request = requests.remove(channelId);
      if (request == null)
         log.warn("No channel request for id: " + channelId + " registered.");
      else
         request.refused(message);
   }

   protected synchronized void channelClosed(Channel channel) {
      try {
         channel.setState(ChannelState.CLOSED);
      } catch (ClosedException e) {
         // Nothing to do here
      }
      channels.remove(channel.getId());
      channelHandler.channelClosed(channel);
   }

   private void newChannelRequest(byte channelId, byte[] initialMessage) {
      MessageType responseType;
      String refuseMessage = null;
      Channel channel = new Channel(channelId, this);
      try {
         channelHandler.newChannelRequested(channel, initialMessage);
         responseType = MessageType.ACCEPT;
         channels.put(channel.getId(), channel);
      } catch (ChannelDeclinedException e) {
         responseType = MessageType.REFUSED;
         refuseMessage = e.getMessage();
      }

      try {
         if (refuseMessage != null)
            send(channelId, responseType, refuseMessage.getBytes(encoding));
         else
            send(channelId, responseType);
      } catch (IOException e) {
         log.warn("Could not send refuse for NewChannelRequestPacket", e);
         return;
      }
   }

   private void ensureOpen() throws ClosedException {
      if (closed)
         throw new ClosedException("Multiplexer has already been closed.");
   }

   /**
    * Get the channel with the given <code>id</code>.
    *
    * @param id The id of the channel
    *
    * @return The channel with the given <code>id</code> if present -
    * <code>null</code> otherwise
    */
   public synchronized Channel getChannel(byte id) {
      return channels.get(id);
   }

   /**
    * Closes this multiplexer and all of its channels.
    */
   public synchronized void close() {
      closed = true;
      Channel[] channels = toArray(this.channels.values(), Channel[]::new);
      for (Channel channel : channels) {
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
   public String toString() {
      return "Multiplexer { channelcount=" + channels.size() + " }";
   }

   // FIXME remove
   private static class Logger {
      void warn(String msg){};
      void warn(String msg, Exception e){};
   }
}