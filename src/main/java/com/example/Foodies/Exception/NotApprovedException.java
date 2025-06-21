package com.example.Foodies.Exception;

public class NotApprovedException extends RuntimeException {
    public NotApprovedException(String message) {
        super(message);
    }
}
