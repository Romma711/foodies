package com.example.Foodies.Usuario.dtos;

public record UsuarioListDTO(
        Long id,
        String nombre,
        String apellido,
        String email,
        String telefono,
        String rol
) { }