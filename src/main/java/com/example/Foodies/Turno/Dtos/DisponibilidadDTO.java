package com.example.Foodies.Turno.Dtos;

import java.time.LocalDate;
import java.util.List;

/** Availability of a restaurant on a date: per shift when it works with shifts ({@code porTurnos}), else per day. */
public record DisponibilidadDTO(LocalDate fecha, boolean porTurnos, List<TurnoDisponibleDTO> turnos) {
}
