package de.dk.ch.de.dk.ch.ex;

import java.io.IOException;

public class InvalidDataException extends IOException {
    private static final long serialVersionUID = -7753883134944246887L;

    private byte[] data;

    public InvalidDataException(String message) {
        super(message);
    }

    public InvalidDataException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a new InvalidDataException with a message and the invalid data.
     *
     * @param message the exceptions message
     * @param data the received invalid data (can be retrieved by {@link #getData()})
     */
    public InvalidDataException(String message, byte[] data) {
        super(message);
        this.data = data;
    }

    /**
     * Creates a new InvalidDataException with a message, the cause and the invalid data.
     *
     * @param message the exceptions message
     * @param cause the cause of the exception
     * @param data the received invalid data (can be retrieved by {@link #getData()})
     */
    public InvalidDataException(String message, Throwable cause, byte[] data) {
        super(message, cause);
        this.data = data;
    }

    /**
     * Get the invalid data, that was received. May be <code>null</code>
     *
     * @return the received invalid data or <code>null</code> if there was no data
     */
    public byte[] getData() {
        return data;
    }
}
