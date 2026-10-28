package com.example.Foodies.Restaurant.Dtos;

import com.example.Foodies.Enums.EspecialidadDeComida;

public record RestaurantListDTO(

        Long id,
        String nombre,
        String ubicacion,
        EspecialidadDeComida especialidad,
        // Map coordinates (Phase 3); null until an admin sets the location.
        Double latitud,
        Double longitud
) {
}
