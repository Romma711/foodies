package com.example.Foodies.Resena.dtos;

import java.time.LocalDateTime;

/** {@code fechaCreacion} is null for reviews written before the date was stored. */
public record ResenaDetailDTO(
        Long id,
        String comentario,
        int calificacion,
        String nombreUsuario,
        String nombreRestaurant,
        LocalDateTime fechaCreacion
) { }
