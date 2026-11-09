package com.example.Foodies.Resena.dtos;

import java.time.LocalDateTime;

/**
 * Reseña vista desde el perfil de quien la escribió: en vez del autor (que es siempre el mismo)
 * trae el restaurante reseñado, para poder mostrarlo y enlazarlo.
 */
public record ResenaUsuarioDTO(
        Long id,
        String comentario,
        int calificacion,
        Long restauranteId,
        String nombreRestaurante,
        LocalDateTime fechaCreacion
) { }
