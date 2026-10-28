package com.example.Foodies.Enums;

/**
 * Estado de la solicitud de alta de un restaurante.
 *
 * <p>Convive con el campo {@code aprobado} de {@link com.example.Foodies.Restaurant.Restaurant}:
 * la entidad los mantiene sincronizados para que el código existente que usa {@code aprobado}
 * siga funcionando.</p>
 */
public enum EstadoRestaurante {
    /** Solicitud recibida, a la espera de que un administrador la revise. */
    PENDIENTE,
    /** Restaurante aprobado y visible en el catálogo público. */
    APROBADO,
    /** Solicitud rechazada por un administrador; no se publica. */
    RECHAZADO
}
