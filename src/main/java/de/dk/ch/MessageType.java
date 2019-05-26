package de.dk.ch;

public enum MessageType {
   DATA(true),
   NEW(false),
   ACCEPT(false),
   CLOSE(false),
   REFUSED(true);

   private final boolean payload;

   private MessageType(boolean payload) {
       this.payload = payload;
   }

    public boolean hasPayload() {
        return payload;
    }
}
