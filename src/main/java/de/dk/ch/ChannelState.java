package de.dk.ch;

/**
 * A {@link Channel} is always in one of these states.
 *
 * @author David Koettlitz
 * <br>Erstellt am 17.07.2017
 */
public enum ChannelState {
   /**
    * This is the initial state of a channel.
    * Indicates that a channel is just opening and not fully established.
    * Messages can maybe not immediately be sent.
    */
   OPENING,
   /**
    * This is the state when a channel has been fully established and is completely operational.
    * Messages can be sent and received.
    */
   OPEN,
   /**
    * This state indicates that a channel has been closed.
    * No messages can be sent or received.
    * A closed channel cannot be opened again.
    */
   CLOSED;
}