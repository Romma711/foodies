package com.example.Foodies.Carta.Dtos;

import java.time.Instant;

public record CartaDTO(
        Long id,
        String nombreArchivo,
        String url,
        Long tamanioBytes,
        Instant actualizadaEn
) {
}
