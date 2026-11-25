package com.example.Foodies.Moderacion.dtos;

import com.example.Foodies.Moderacion.HistorialStrike;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/** One entry of the strike history as the admin sees it (S7d). */
public record HistorialStrikeDTO(
        Long id,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime fecha,
        HistorialStrike.Tipo tipo,
        HistorialStrike.Origen origen,
        String motivo,
        Integer totalDespues
) {
    public static HistorialStrikeDTO from(HistorialStrike h) {
        return new HistorialStrikeDTO(h.getId(), h.getFecha(), h.getTipo(), h.getOrigen(), h.getMotivo(), h.getTotalDespues());
    }
}
