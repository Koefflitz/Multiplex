package de.dk.ch;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;

/**
 * This is the interface to receive messages.
 *
 * @author David Koettlitz
 * <br>Erstellt am 13.07.2017
 *
 * @see Multiplexer
 */
public interface Receiver {

   /**
    * Handles a received message.
    *
    * @param data The received message
    *
    * @throws IllegalArgumentException If the message could not be handled
    */
   public void receive(byte[] data) throws IllegalArgumentException;

   public static class ReceiverChain extends HashSet<Receiver> implements Receiver {
      private static final long serialVersionUID = 4570474656007106847L;

      @Override
      public void receive(byte[] data) throws IllegalArgumentException {
         Receiver[] receivers = toArray(new Receiver[size()]);
         Collection<IllegalArgumentException> exceptions = new LinkedList<>();
         if (receivers.length == 0)
            return;

         boolean success = false;
         for (Receiver receiver : receivers) {
            try {
               receiver.receive(data);
               success = true;
            } catch (IllegalArgumentException e) {
               exceptions.add(e);
            }
         }
         if (success)
            return;

         String errorMsg = "No receiver could handle the received message: " + data;
         IllegalArgumentException finalException = new IllegalArgumentException(errorMsg);
         for (IllegalArgumentException e : exceptions)
            finalException.addSuppressed(e);

         throw finalException;
      }

   }
}