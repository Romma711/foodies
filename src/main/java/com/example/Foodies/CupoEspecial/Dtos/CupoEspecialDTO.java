package com.example.Foodies.CupoEspecial.Dtos;

import java.time.LocalDate;

/** Cupo especial de un espacio un día, con las personas ya reservadas (no canceladas) en ese espacio ese día. */
public record CupoEspecialDTO(
        LocalDate fecha,
        Integer cupo,
        String motivo,
        Integer reservadas,
        Long idEspacio,
        String espacioNombre
) {
}
