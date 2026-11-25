package com.example.Foodies.Exception;

import java.util.Map;

public class NotValidCupoException extends ErrorConCodigo {
    public NotValidCupoException(String message) {
        super(null, Map.of(), message);
    }

    public NotValidCupoException(String codigo, Map<String, Object> params, String message) {
        super(codigo, params, message);
    }
}
