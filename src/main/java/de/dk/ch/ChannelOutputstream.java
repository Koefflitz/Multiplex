package de.dk.ch;

import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.Queue;

import static de.dk.ch.ChannelState.OPEN;
import static de.dk.ch.ChannelState.OPENING;

public final class ChannelOutputstream extends OutputStream {
   private static final int DEFAULT_SIZE = 8192;

   private final Multiplexer multiplexer;
   private final Channel channel;

   private final byte[] buffer;
   private final int size;
   private int index = 0;

   private Queue<byte[]> preSentMessages = new LinkedList<>();

   ChannelOutputstream(Multiplexer multiplexer, Channel channel, int bufferSize) {
      this.multiplexer = multiplexer;
      this.channel = channel;
      this.size = bufferSize;
      this.buffer = new byte[size];
   }

   ChannelOutputstream(Multiplexer multiplexer, Channel channel) {
      this(multiplexer, channel, DEFAULT_SIZE);
   }

   @Override
   public void write(int b) throws IOException {
      channel.ensureNotClosed();

      buffer[index++] = (byte) b;
      if (index == size)
         flush();
   }

   @Override
   public void write(byte[] b, int off, int len) throws IOException {
      channel.ensureNotClosed();


      if (channel.getState() == OPEN)
         if (len > size - index) {
            byte[] data = new byte[index + len];
            System.arraycopy(buffer, 0, data, 0, index);
            System.arraycopy(b, off, data, size, len);
            multiplexer.send(channel.getId(), MessageType.DATA, data);
            index = 0;
         } else {
            System.arraycopy(b, off, buffer, index, len);
         }
      else if (channel.getState() == OPENING) {
         byte[] data = new byte[len];
         System.arraycopy(b, off, data, 0, len);
         preSentMessages.offer(data);
      }
   }

   void sendQueuedMessages() throws IOException {
      while (!preSentMessages.isEmpty())
         multiplexer.send(channel.getId(), MessageType.DATA, preSentMessages.poll());

      preSentMessages = null;
   }

   @Override
   public void flush() throws IOException {
      channel.ensureNotClosed();
      if (preSentMessages != null && !preSentMessages.isEmpty()) {
         while (!preSentMessages.isEmpty())
            multiplexer.send(channel.getId(), MessageType.DATA, preSentMessages.poll());
      }

      preSentMessages = null;
      multiplexer.send(channel.getId(), MessageType.DATA, buffer, 0, index);
   }
}
