package com.example.Foodies.Exception;

/** The request clashes with the current state of the data (HTTP 409), e.g. a duplicate reservation. */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
