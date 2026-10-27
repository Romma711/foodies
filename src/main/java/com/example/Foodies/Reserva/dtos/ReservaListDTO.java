package com.example.Foodies.Reserva.dtos;

import com.example.Foodies.Enums.EstadoReserva;

import java.time.LocalTime;
import java.util.Date;

public record ReservaListDTO(
        Long id,
        Integer cantidad,
        Date fechaReserva,
        LocalTime horariollegada,
        EstadoReserva estadoReserva,
        String nombreResto
) { }
