package com.example.Foodies.Turno.Dtos;

import com.example.Foodies.Turno.Turno;

import java.time.DayOfWeek;
import java.time.format.DateTimeFormatter;

/** A shift as the panel and the public list show it; times are HH:mm and {@code fin} = inicio + duracion. */
public record TurnoDTO(Long id, DayOfWeek dia, String inicio, String fin, Integer duracionMinutos) {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    public static TurnoDTO from(Turno turno) {
        return new TurnoDTO(turno.getId(), turno.getDia(), turno.getInicio().format(HORA), turno.fin().format(HORA),
                turno.getDuracionMinutos());
    }
}
