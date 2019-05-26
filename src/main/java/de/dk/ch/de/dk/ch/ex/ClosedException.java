package de.dk.ch.de.dk.ch.ex;

import de.dk.ch.Channel;
import de.dk.ch.Multiplexer;

import java.io.IOException;

/**
 * Indicates that a channel has been closed.
 *
 * @author David Koettlitz
 * <br>Erstellt am 13.07.2017
 *
 * @see Multiplexer
 * @see Channel
 */
public class ClosedException extends IOException {
   private static final long serialVersionUID = 4162162428990881318L;

   public ClosedException(String message) {
      super(message);
   }

   public ClosedException(Throwable cause) {
      super(cause);
   }

   public ClosedException(String message, Throwable cause) {
      super(message, cause);
   }
}