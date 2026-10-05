package com.example.Foodies.Reserva.dtos;

import com.example.Foodies.Enums.EstadoReserva;

import java.time.LocalDate;
import java.time.LocalTime;

public record ReservaDetailDTO(
    Long id,
    Integer cantidad,
    LocalDate fecha,
    LocalTime horariollegada,
    EstadoReserva estadoReserva,
    String nombreUsuario,
    String nombreResto
) { }
