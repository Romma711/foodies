package com.example.Foodies.Reserva.dtos;

import com.example.Foodies.Enums.EstadoReserva;

import java.time.LocalTime;

public record ReservaListDTO(
        Long id,
        Integer cantidad,
        LocalTime horariollegada,
        EstadoReserva estadoReserva
) { }
