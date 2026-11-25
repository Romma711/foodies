package com.example.Foodies.Turno.Dtos;

import java.util.List;

/**
 * One bookable slot of a day: a shift, or the whole day ({@code idTurno}, {@code inicio} and {@code fin} null)
 * for a restaurant without shifts.
 */
public record TurnoDisponibleDTO(Long idTurno, String inicio, String fin, List<EspacioDisponibleDTO> espacios) {
}
