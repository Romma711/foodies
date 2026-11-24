package com.example.Foodies.Exception;

import java.util.Map;

public class BusinessException extends ErrorConCodigo {
    public BusinessException(String message) {
        super(null, Map.of(), message);
    }

    public BusinessException(String codigo, Map<String, Object> params, String message) {
        super(codigo, params, message);
    }
}
