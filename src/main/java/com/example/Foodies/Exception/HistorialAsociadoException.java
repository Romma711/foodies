package com.example.Foodies.Exception;

/**
 * Se lanza cuando no se puede eliminar algo porque tiene historial asociado
 * (reservas, reseñas). Se responde con 409 Conflict.
 */
public class HistorialAsociadoException extends RuntimeException {
    public HistorialAsociadoException(String message) {
        super(message);
    }
}
