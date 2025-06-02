package com.example.Foodies.Reserva.dtos;

import com.example.Foodies.Enums.EstadoReserva;

import java.time.LocalTime;

public record ReservaDetailDTO(
    Long id,
    Long cantidad,
    LocalTime horariollegada,
    LocalTime horariofin,
    EstadoReserva estadoReserva,
    String nombreCliente,
    String nombreResto
) { }
