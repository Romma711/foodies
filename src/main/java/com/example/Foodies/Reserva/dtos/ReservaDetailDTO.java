package com.example.Foodies.Reserva.dtos;

import com.example.Foodies.Enums.EstadoReserva;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Date;

public record ReservaDetailDTO(
    Long id,
    Integer cantidad,
    Date fecha,
    LocalTime horariollegada,
    EstadoReserva estadoReserva,
    String nombreCliente,
    String nombreResto
) { }
