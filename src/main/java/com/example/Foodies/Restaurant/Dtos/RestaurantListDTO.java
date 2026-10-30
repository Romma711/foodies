package com.example.Foodies.Restaurant.Dtos;

import com.example.Foodies.Enums.EspecialidadDeComida;

public record RestaurantListDTO(

        Long id,
        String nombre,
        String ubicacion,
        EspecialidadDeComida especialidad,
        /** URL de la portada, o null si el restaurante todavía no cargó fotos. */
        String fotoPortada
) {
}
