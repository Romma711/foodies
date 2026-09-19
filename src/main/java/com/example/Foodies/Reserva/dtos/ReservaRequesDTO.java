package com.example.Foodies.Reserva.dtos;

import com.example.Foodies.Enums.EstadoReserva;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReservaRequesDTO {
    private Integer cantidad;
    private EstadoReserva estadoReserva;
    private String fechaReserva;
    private String horarioLlegada;
    private Long idUsuario;
    private Long idRestaurant;
}