package com.example.Foodies.Moderacion.dtos;

import jakarta.validation.constraints.NotNull;

/**
 * Cambio de la baja lógica.
 *
 * @param activo {@code false} da de baja; {@code true} reactiva
 */
public record ActivoRequestDTO(
        @NotNull(message = "Indicá si la entidad queda activa")
        Boolean activo
) {
}
