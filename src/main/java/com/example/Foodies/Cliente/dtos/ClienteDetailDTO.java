package com.example.Foodies.Cliente.dtos;

public record ClienteDetailDTO(
        Long idUsuario,
        Long idCliente,
        String email,
        String nombre,
        String apellido,
        String telefono
) {
}
