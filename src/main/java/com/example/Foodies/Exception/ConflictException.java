package com.example.Foodies.Exception;

import java.util.Map;

/** The request clashes with the current state of the data (HTTP 409), e.g. a duplicate reservation. */
public class ConflictException extends ErrorConCodigo {
    public ConflictException(String message) {
        super(null, Map.of(), message);
    }

    public ConflictException(String codigo, Map<String, Object> params, String message) {
        super(codigo, params, message);
    }
}
