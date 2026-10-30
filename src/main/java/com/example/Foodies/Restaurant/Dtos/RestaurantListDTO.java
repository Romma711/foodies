package com.example.Foodies.Restaurant.Dtos;

import com.example.Foodies.Enums.EspecialidadDeComida;

public record RestaurantListDTO(

        Long id,
        String nombre,
        String ubicacion,
        EspecialidadDeComida especialidad,
        /** URL de la foto del frente, o null si el restaurante todavía no la cargó. */
        String fotoPortada
) {
}
