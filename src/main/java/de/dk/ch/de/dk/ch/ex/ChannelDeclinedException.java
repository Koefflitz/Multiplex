package de.dk.ch.de.dk.ch.ex;

/**
 * Thrown to indicate that a request to open a new channel has been declined
 *
 * @author David Koettlitz
 * <br>Erstellt am 13.07.2017
 */
public class ChannelDeclinedException extends Exception {
   private static final long serialVersionUID = -4436729764447648915L;

   private static final String MSG = "The request to open a new channel was declined.";

   public ChannelDeclinedException() {
      super(MSG);
   }

   public ChannelDeclinedException(String message) {
      super(MSG + " Reason: " + message);
   }
}