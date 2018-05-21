package de.dk.ch;

import static de.dk.ch.ChannelState.CLOSED;
import static de.dk.ch.ChannelState.OPEN;
import static de.dk.ch.ChannelState.OPENING;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dk.ch.ChannelListener.ChannelListenerChain;
import de.dk.ch.ChannelPacket.ChannelPacketType;

/**
 * A channel through which messages can be send and received.
 *
 * @author David Koettlitz
 * <br>Erstellt am 13.07.2017
 *
 * @see Multiplexer
 */
public class Channel<T> {
   private static final Logger log = LoggerFactory.getLogger(Channel.class);

   private final long id;
   private final ChannelListenerChain<T> listeners = new ChannelListenerChain<>();
   private final Sender sender;
   private final Multiplexer multiplexer;
   private Queue<T> preSentMessages = new LinkedList<>();

   private ChannelState state = OPENING;

   /**
    * Creates a new channel with the given id, sender and the multiplexer that manages this channel.
    * It is recommended to use a multiplexer to create new channels.
    *
    * @param id The id of this channel (should be unique)
    * @param sender The sender via which messages are sent (cannot be <code>null</code>)
    * @param multiplexer The multiplexer that manages this channel
    *
    * @throws NullPointerException If the given <code>sender</code> is <code>null</code>
    */
   public Channel(long id, Sender sender, Multiplexer multiplexer) throws NullPointerException {
      this.id = id;
      this.sender = Objects.requireNonNull(sender);
      this.multiplexer = multiplexer;
   }

   /**
    * This method is usually called by the multiplexer when a packet with this channelId arrived.
    * This method can manually be called to fake an arrival of a packet for this channel.
    *
    * @param packet The arrived packet
    *
    * @throws IllegalArgumentException If the packet has not the same <code>channelId</code> as this channel
    * @throws ClosedException If this channel has already been closed
    */
   @SuppressWarnings("unchecked")
   public synchronized void receive(PayloadPacket packet) throws IllegalArgumentException,
                                                                 ClosedException {
      if (packet.channelId != this.id)
         throw new IllegalArgumentException("Packet ID does not match this channel id");

      ensureNotClosed();
      synchronized (listeners) {
         listeners.received((T) packet.getPayload());
      }
   }

   /**
    * Sends an object via the sender through this channel.
    * Objects can only be sent if this channels state is <code>OPEN</code>.
    * If the channel is in <code>OPENING</code> state the messages will be queued until it is opened.
    * Queued messages will be sent when the channel has been opened.
    * If the channel never opens, e.g. the request gets declined, the queued messages will never be delivered.
    * Therefore it is recommended to send messages in <code>OPEN</code> state.
    *
    * @param object The object to be sent
    *
    * @throws ClosedException If this channel has already been closed
    * @throws IOException If an I/O error occurs while sending the object
    */
   public synchronized void send(T object) throws ClosedException, IOException {
      ensureNotClosed();
      if (state == OPEN)
         sender.send(new PayloadPacket(id, object));
      else if (state == OPENING)
         preSentMessages.offer(object);
   }

   protected synchronized void send(ChannelPacket packet) throws IOException, ClosedException {
      ensureNotClosed();
      sender.send(packet);
   }

   /**
    * Waits for this channel to be opened.
    *
    * @param timeout The waiting timeout in milliseconds
    *
    * @throws InterruptedException If the calling thread is interrupted while waiting
    */
   public synchronized void waitToOpen(long timeout) throws InterruptedException {
      wait(timeout);
   }

   private void ensureNotClosed() throws ClosedException {
      if (isClosed())
         throw new ClosedException("This channel has already been closed.");
   }

   private void sendQueuedMessages() {
      try {
         while (!preSentMessages.isEmpty())
            send(preSentMessages.poll());

      } catch (IllegalArgumentException | IOException e) {
         log.warn("Could not send prequeued messages");
      }
      preSentMessages = null;
   }

   /**
    * Adds a channel listener to this channel.
    * The channel listener is called when a message arrives through this channel.
    *
    * @param listener The listener to be added to this channel
    */
   public void addListener(ChannelListener<T> listener) {
      synchronized (listeners) {
         listeners.add(listener);
      }
   }

   /**
    * Removes the given <code>listener</code> from this channel.
    *
    * @param listener The listener to be removed
    */
   public void removeListener(ChannelListener<T> listener) {
      synchronized (listeners) {
         listeners.remove(listener);
      }
   }

   /**
    * Closes this channel.
    * Note: A once closed channel cannot be reopened.
    *
    * @throws IOException If an I/O error occurs while closing the channel
    */
   public synchronized void close() throws IOException {
      if (state == CLOSED)
         return;

      try {
         send(new ChannelPacket(id, ChannelPacketType.CLOSE));
      } finally {
         state = CLOSED;

         if (multiplexer != null)
            multiplexer.channelClosed(this);
      }
   }

   protected synchronized void setState(ChannelState state) throws ClosedException {
      if (state == this.state)
         return;

      if (this.state == ChannelState.CLOSED)
         throw new ClosedException("Channel has already been closed.");

      this.state = state;
      if (state == OPEN) {
         sendQueuedMessages();
         notifyAll();
      }
   }

   public Iterable<ChannelListener<T>> getListeners() {
      return listeners;
   }

   /**
    * Get the closed state of this channel.
    * Messages can only be delivered through an open channel.
    * If this method returns <code>true</code>, the send and receive methods
    * of this channel will throw a <code>ClosedException</code>.
    *
    * @return <code>true</code> if this channel is closed.
    * <code>false</code> otherwise
    */
   public synchronized boolean isClosed() {
      return state == CLOSED;
   }

   /**
    * Get the state of this channel.
    *
    * @return The state of this channel
    */
   public synchronized ChannelState getState() {
      return state;
   }

   /**
    * Get the id of this channel.
    *
    * @return The unique id of this channel
    */
   public long getId() {
      return id;
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + (int) (this.id ^ (this.id >>> 32));
      result = prime * result + ((this.state == null) ? 0 : this.state.hashCode());
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
      Channel<?> other = (Channel<?>) obj;
      if (this.id != other.id)
         return false;
      if (this.state != other.state)
         return false;
      return true;
   }

   @Override
   public String toString() {
      return "channel { id=" + id + ", state=" + state + " }";
   }
}