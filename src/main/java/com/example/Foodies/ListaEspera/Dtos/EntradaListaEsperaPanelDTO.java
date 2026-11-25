package com.example.Foodies.ListaEspera.Dtos;

import com.example.Foodies.ListaEspera.EstadoListaEspera;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A waitlist entry as the restaurant panel sees it (S7d, D23); {@code horario} is HH:mm and
 * {@code cliente} the client's name, or its email when it has none.
 */
public record EntradaListaEsperaPanelDTO(Long id, LocalDate fecha, String horario, Long idTurno, Integer cantidad,
                                         EstadoListaEspera estado,
                                         @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime creada,
                                         String cliente) {
}
