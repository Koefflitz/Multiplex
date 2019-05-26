package de.dk.ch;

import java.util.NoSuchElementException;

/**
 * A class to generate unique ids.
 *
 * @author David Koettlitz
 * <br>Erstellt am 13.07.2017
 */
public interface IDGenerator {
   /**
    * Get the next unique id.
    * Each call of this method generates a new unique id.
    *
    * @return A fresh new generated unique id
    *
    * @throws NoSuchElementException If no more id can be generated
    */
   public byte nextId() throws NoSuchElementException;
}