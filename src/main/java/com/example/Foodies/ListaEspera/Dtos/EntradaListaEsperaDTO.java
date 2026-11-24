package com.example.Foodies.ListaEspera.Dtos;

import com.example.Foodies.ListaEspera.EstadoListaEspera;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** A waitlist entry as its client sees it; {@code horario} is HH:mm. */
public record EntradaListaEsperaDTO(Long id, Long idRestaurant, String nombreResto, LocalDate fecha, String horario,
                                    Long idTurno, Integer cantidad, EstadoListaEspera estado, LocalDateTime creada) {
}
