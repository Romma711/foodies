package com.example.Foodies.CupoEspecial.Dtos;

import java.time.LocalDate;

/** Cupo especial de un día, con las personas ya reservadas (no canceladas) para ese día. */
public record CupoEspecialDTO(
        LocalDate fecha,
        Integer cupo,
        String motivo,
        Integer reservadas
) {
}
