package com.example.Foodies.Exception;

import java.util.Map;

/**
 * Error with a stable machine-readable {@code codigo} and the values its message interpolates
 * ({@code params}), so a front in another language can translate it. The Spanish {@code mensaje}
 * stays as it was. Errors created without a code answer as before.
 */
public abstract class ErrorConCodigo extends RuntimeException {

    private final String codigo;
    private final Map<String, Object> params;

    protected ErrorConCodigo(String codigo, Map<String, Object> params, String message) {
        super(message);
        this.codigo = codigo;
        this.params = params == null ? Map.of() : params;
    }

    public String getCodigo() {
        return codigo;
    }

    public Map<String, Object> getParams() {
        return params;
    }
}
