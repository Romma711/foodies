package com.example.Foodies.Turno.Dtos;

import java.time.DayOfWeek;

/**
 * Body to create or change a shift.
 *
 * @param dia             day of the week, e.g. SATURDAY
 * @param inicio          start time HH:mm (the arrival time of its reservations)
 * @param duracionMinutos length in minutes; null means 90
 */
public record TurnoRequestDTO(DayOfWeek dia, String inicio, Integer duracionMinutos) {
}
