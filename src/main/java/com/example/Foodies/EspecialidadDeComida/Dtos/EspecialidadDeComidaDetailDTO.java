package com.example.Foodies.EspecialidadDeComida.Dtos;

import com.example.Foodies.Enums.TipoDeComida;

public record EspecialidadDeComidaDetailDTO(
        Long id,
        TipoDeComida tipoDeComida
) {
}
