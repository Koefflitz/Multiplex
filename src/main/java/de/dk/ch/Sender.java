package de.dk.ch;

import java.io.IOException;
import java.io.Serializable;

/**
 * Something that can send a serializable object.
 * This interface is used by the {@link Multiplexer} class to send messages through channnels.
 *
 * @author David Koettlitz
 * <br>Erstellt am 13.07.2017
 *
 * @see Multiplexer
 * @see Channel
 */
public interface Sender {
   /**
    * Sends the given object.
    *
    * @param msg The message to be send.
    *
    * @throws IOException If an I/O error occurs
    */
   public void send(Serializable msg) throws IOException;
}