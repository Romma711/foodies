package com.example.Foodies.Cliente.dtos;

public record ClienteListDTO(
        Long id,
        String nombre,
        String apellido,
        String email
) { }
