package com.example.Foodies.Restaurant.Dtos;

import com.example.Foodies.Carta.Dtos.CartaDTO;
import com.example.Foodies.Enums.EspecialidadDeComida;

/** The manager's own restaurant, as shown in their panel. `carta` is null until a menu is uploaded. */
public record MiRestauranteDTO(
        Long id,
        String nombre,
        Integer cupoMaximo,
        Integer porcentajeAutoAceptacion,
        String ubicacion,
        EspecialidadDeComida especialidad,
        CartaDTO carta
) {
}
