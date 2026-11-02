package com.example.Foodies.Moderacion.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos para registrar un strike.
 *
 * @param motivo explicación que recibe el sancionado en el email de aviso
 */
public record StrikeRequestDTO(
        @NotBlank(message = "El motivo es obligatorio")
        @Size(min = 5, max = 255, message = "El motivo debe tener entre 5 y 255 caracteres")
        String motivo
) {
}
