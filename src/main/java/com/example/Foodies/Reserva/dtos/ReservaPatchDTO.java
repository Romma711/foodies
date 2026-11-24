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
    /** New espacio of the same restaurant; null keeps the current one. */
    private Long idEspacio;
    /** New shift of the same restaurant (RF24); null keeps it or picks the one starting at horarioLlegada. */
    private Long idTurno;
}