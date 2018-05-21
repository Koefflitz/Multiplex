package de.dk.ch;

/**
 * 11.05.2018
 */
public class TestObject {
   public static final String DEFAULT_MSG = "TestObject.";

   private String message;

   public TestObject(String message) {
      this.message = message;
   }

   public static TestObject defaultMessage() {
      return new TestObject(DEFAULT_MSG);
   }

   public String getMessage() {
      return message;
   }

   public void setMessage(String message) {
      this.message = message;
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((message == null) ? 0 : message.hashCode());
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
      TestObject other = (TestObject) obj;
      if (message == null) {
         if (other.message != null)
            return false;
      } else if (!message.equals(other.message))
         return false;
      return true;
   }

   @Override
   public String toString() {
      return message;
   }
}
