package de.dk.ch;

import java.util.Optional;

import de.dk.ch.de.dk.ch.ex.ChannelDeclinedException;


/**
 * An instance for handling requests for new channels and closes of channels.
 * A <code>ChannelHandler</code> can be registered at a {@link Multiplexer}
 * and will be informed whenever a request to open a channel
 * is received or when a <code>Channel</code> is closed.
 *
 * @author David Koettlitz
 * <br>Erstellt am 14.07.2017
 *
 * @see Multiplexer
 */
public interface ChannelHandler {
   /**
    * This method is called whenever a request to open a new channel is requested.
    * To decline the request a <code>ChannelDeclinedException</code> is thrown.
    * Otherwise the channel will be opened to be ready for communication.
    *
    * @param channel The channel that is requested to be opened
    * @param initialMessage An optional initial message that came with the request (may be <code>null</code>).
    *
    * @throws ChannelDeclinedException If the channel should not be opened
    */
   public void newChannelRequested(Channel channel, byte[] initialMessage) throws ChannelDeclinedException;

   /**
    * This method is called when a channel has been closed.
    *
    * @param channel The closed channel
    */
   public void channelClosed(Channel channel);
}