package com.example.Foodies.Usuario.dtos;

import com.example.Foodies.Foto.Dtos.FotoDTO;

import java.util.List;

public record UsuarioRestoAAprobarDTO(
        Long id,
        String nombre,
        String email,
        String telefono,
        String rol,
        List<FotoDTO> fotos
) {
}
