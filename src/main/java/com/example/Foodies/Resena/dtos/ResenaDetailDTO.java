package com.example.Foodies.Resena.dtos;

public record ResenaDetailDTO(
        Long id,
        String comentario,
        int calificacion,
        Long idCliente,
        Long idRestaurant
) { }
