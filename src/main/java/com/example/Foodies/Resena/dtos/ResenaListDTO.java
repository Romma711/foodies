package com.example.Foodies.Resena.dtos;

public record ResenaListDTO(
    Long id,
    String comentario,
    int calificacion,
    String nombreCliente,
    String nombreRestaurant
) { }
