package com.example.Foodies.Exception;

import java.util.Map;

public class ForbiddenException extends ErrorConCodigo {
    public ForbiddenException(String message) {
        super(null, Map.of(), message);
    }

    public ForbiddenException(String codigo, Map<String, Object> params, String message) {
        super(codigo, params, message);
    }
}
