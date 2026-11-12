package com.example.Foodies.Reserva.dtos;

import com.example.Foodies.Enums.EstadoReserva;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReservaPatchDTO {
    /** New party size; null keeps the current one. */
    private Integer cantidad;
    /** Only {@code CANCELADA} is accepted from a client; when present the other fields are ignored. */
    private EstadoReserva estadoReserva;
    /** New date, {@code yyyy-MM-dd}; null keeps the current one. */
    private String fechaReserva;
    /** New arrival time, {@code HH:mm}; null keeps the current one. */
    private String horarioLlegada;
}