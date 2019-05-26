package de.dk.ch;

import static de.dk.ch.ChannelState.CLOSED;
import static de.dk.ch.ChannelState.OPEN;
import static de.dk.ch.ChannelState.OPENING;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;

import de.dk.ch.de.dk.ch.ex.ClosedException;

import de.dk.ch.ChannelListener.ChannelListenerChain;

/**
 * A channel through which messages can be send and received.
 *
 * @author David Koettlitz
 * <br>Erstellt am 13.07.2017
 *
 * @see Multiplexer
 */
public class Channel {
   private final byte id;
   private final ChannelListenerChain listeners = new ChannelListenerChain();
   private final Multiplexer multiplexer;
   private ChannelOutputstream outputstream;

   private ChannelState state = OPENING;

   /**
    * Creates a new channel with the given id, sender and the multiplexer that manages this channel.
    * It is recommended to use a multiplexer to create new channels.
    *
    * @param id The id of this channel (should be unique)
    * @param multiplexer The multiplexer that manages this channel
    *
    * @throws NullPointerException If the given <code>sender</code> is <code>null</code>
    */
   Channel(byte id, Multiplexer multiplexer) throws NullPointerException {
      this.id = id;
      this.multiplexer = multiplexer;
      this.outputstream = new ChannelOutputstream(multiplexer, this);
   }

   /**
    * This method is usually called by the multiplexer when a packet with this channelId arrived.
    * This method can manually be called to fake an arrival of a packet for this channel.
    *
    * @param data The arrived data
    *
    * @throws IllegalArgumentException If the packet has not the same <code>channelId</code> as this channel
    * @throws ClosedException If this channel has already been closed
    */
   public synchronized void receive(byte[] data) throws IllegalArgumentException,
                                                        ClosedException {
      ensureNotClosed();
      synchronized (listeners) {
         listeners.received(data);
      }
   }

   /**
    * Waits for this channel to be opened.
    *
    * @param timeout The waiting timeout in milliseconds
    *
    * @throws InterruptedException If the calling thread is interrupted while waiting
    */
   public synchronized void waitToOpen(long timeout) throws InterruptedException {
      if (state != OPEN)
         wait(timeout);
   }

   void ensureNotClosed() throws ClosedException {
      if (isClosed())
         throw new ClosedException("This channel has already been closed.");
   }

   /**
    * Adds a channel listener to this channel.
    * The channel listener is called when a message arrives through this channel.
    *
    * @param listener The listener to be added to this channel
    */
   public void addListener(ChannelListener listener) {
      synchronized (listeners) {
         listeners.add(listener);
      }
   }

   /**
    * Removes the given <code>listener</code> from this channel.
    *
    * @param listener The listener to be removed
    */
   public void removeListener(ChannelListener listener) {
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
         multiplexer.send(id, MessageType.CLOSE);
      } finally {
         state = CLOSED;
         outputstream = null;

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
         try {
            outputstream.sendQueuedMessages();
         } catch (IOException e) {
            // Can't handle, can't
            e.printStackTrace();
         }
         notifyAll();
      }
   }

   public synchronized OutputStream getOutputstream() throws ClosedException {
      ensureNotClosed();
      return outputstream;
   }

   public Iterable<ChannelListener> getListeners() {
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
   public byte getId() {
      return id;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o)
         return true;
      if (o == null || getClass() != o.getClass())
         return false;
      Channel channel = (Channel) o;
      return id == channel.id;
   }

   @Override
   public int hashCode() {
      return Objects.hash(id);
   }

   @Override
   public String toString() {
      return "channel { id=" + id + ", state=" + state + " }";
   }
}