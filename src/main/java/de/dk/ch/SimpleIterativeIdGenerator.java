package de.dk.ch;

import java.util.NoSuchElementException;

/**
 * A simple id generator that generates ids from <code>Long.MIN_VALUE</code> to <code>Long.MAX_VALUE</code>
 * or vice versa.
 *
 * @author David Koettlitz
 * <br>Erstellt am 14.07.2017
 */
public class SimpleIterativeIdGenerator implements IDGenerator {
   private final boolean incrementing;
   private long idCounter;

   /**
    * Creates a new simple id generator that generates ids from <code>Long.MIN_VALUE</code>
    * to <code>Long.MAX_VALUE</code>.
    */
   public SimpleIterativeIdGenerator() {
      this(true);
   }

   /**
    * Creates a new simple id generator that generates ids from <code>Long.MIN_VALUE</code>
    * to <code>Long.MAX_VALUE</code> or vice versa.
    *
    * @param incrementing If <code>true</code> this id generator will produce ids from
    * <code>Long.MIN_VALUE</code> to <code>Long.MAX_VALUE</code><br>
    * <code>false</code> for <code>Long.MAX_VALUE</code> to <code>Long.MIN_VALUE</code>.
    */
   public SimpleIterativeIdGenerator(boolean incrementing) {
      this.incrementing = incrementing;
      this.idCounter = incrementing ? Long.MIN_VALUE : Long.MAX_VALUE;
   }

   @Override
   public long nextId() throws NoSuchElementException {
      if (idCounter == (incrementing ? Long.MAX_VALUE : Long.MIN_VALUE))
         throw new NoSuchElementException();

      return incrementing ? idCounter++ : idCounter--;
   }

   public boolean isIncrementing() {
      return incrementing;
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + (int) (this.idCounter ^ (this.idCounter >>> 32));
      result = prime * result + (this.incrementing ? 1231 : 1237);
      return result;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj)
         return true;
      if (obj == null)
         return false;
      if (getClass() != obj.getClass())
         return false;
      SimpleIterativeIdGenerator other = (SimpleIterativeIdGenerator) obj;
      if (this.idCounter != other.idCounter)
         return false;
      if (this.incrementing != other.incrementing)
         return false;
      return true;
   }

   @Override
   public String toString() {
      return "SimpleIterativeIdGenerator { idCounter=" + idCounter + ", incrementing=" + incrementing + " }";
   }
}