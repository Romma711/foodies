package com.example.Foodies.Exception;

import java.util.Map;

public class EntityNotFoundException extends ErrorConCodigo {
    public EntityNotFoundException(String message) {
        super(null, Map.of(), message);
    }

    public EntityNotFoundException(String codigo, Map<String, Object> params, String message) {
        super(codigo, params, message);
    }
}
