package com.example.Foodies.Restaurant.Dtos;

import com.example.Foodies.Enums.EspecialidadDeComida;

public record RestaurantDetailDTO(
        Long id,
        String nombre,
        Integer cupoMaximo,
        String ubicacion,
        EspecialidadDeComida especialidad

) {
}
