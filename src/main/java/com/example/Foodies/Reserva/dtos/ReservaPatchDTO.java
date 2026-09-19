package com.example.Foodies.Reserva.dtos;

import com.example.Foodies.Enums.EstadoReserva;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReservaPatchDTO {
    private Integer cantidad;
    private EstadoReserva estadoReserva;
}