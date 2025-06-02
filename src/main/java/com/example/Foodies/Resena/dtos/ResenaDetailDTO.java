package com.example.Foodies.Resena.dtos;

public record ResenaDetailDTO(
        Long id,
        String comentario,
        int calificacion,
        String nombreCliente,
        String nombreRestaurant
) { }
