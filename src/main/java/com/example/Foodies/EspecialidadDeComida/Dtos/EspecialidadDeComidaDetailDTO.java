package com.example.Foodies.EspecialidadDeComida.Dtos;

import com.example.Foodies.Enums.EspecialidadDeComida;

public record EspecialidadDeComidaDetailDTO(
        Long id,
        EspecialidadDeComida tipoDeComida
) {
}
