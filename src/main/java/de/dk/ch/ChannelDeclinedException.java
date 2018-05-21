package de.dk.ch;

/**
 * Thrown to indicate that a request to open a new channel has been declined
 *
 * @author David Koettlitz
 * <br>Erstellt am 13.07.2017
 */
public class ChannelDeclinedException extends Exception {
   private static final long serialVersionUID = -4436729764447648915L;

   public ChannelDeclinedException() {

   }

   public ChannelDeclinedException(String message) {
      super(message);
   }

   public ChannelDeclinedException(Throwable cause) {
      super(cause);
   }

   public ChannelDeclinedException(String message, Throwable cause) {
      super(message, cause);
   }
}