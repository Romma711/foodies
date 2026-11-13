package com.example.Foodies.Horario.Dtos;

import java.time.DayOfWeek;

/**
 * Franja de atención de un día, en el panel y en la vista pública. Las horas son {@code HH:mm};
 * un {@code cierre} {@code "00:00"} significa medianoche.
 *
 * @param dia      día de la semana ({@code MONDAY} … {@code SUNDAY})
 * @param apertura desde qué hora se reciben llegadas
 * @param cierre   hora de cierre; a esa hora ya no se reciben llegadas
 */
public record FranjaHorariaDTO(DayOfWeek dia, String apertura, String cierre) {
}
