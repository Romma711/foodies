package com.example.Foodies.Resena.dtos;

import java.time.LocalDateTime;

/** {@code fechaCreacion} is null for reviews written before the date was stored. */
public record ResenaListDTO(
    Long id,
    String nombreUsuario,
    String comentario,
    int calificacion,
    LocalDateTime fechaCreacion
) { }
