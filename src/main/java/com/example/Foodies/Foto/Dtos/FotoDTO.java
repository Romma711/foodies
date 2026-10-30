package com.example.Foodies.Foto.Dtos;

import com.example.Foodies.Foto.TipoFoto;

public record FotoDTO(
        Long id,
        TipoFoto tipo,
        String url
) {
}
