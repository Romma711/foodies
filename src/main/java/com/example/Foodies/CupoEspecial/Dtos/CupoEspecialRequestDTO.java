package com.example.Foodies.CupoEspecial.Dtos;

/** Cuerpo de `PUT /api/restaurantes/me/cupos-especiales/{fecha}`. `motivo` es opcional. */
public record CupoEspecialRequestDTO(
        Integer cupo,
        String motivo
) {
}
