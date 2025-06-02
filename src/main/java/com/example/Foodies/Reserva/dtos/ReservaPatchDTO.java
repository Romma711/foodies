package com.example.Foodies.Reserva.dtos;

import com.example.Foodies.Enums.EstadoReserva;

public class ReservaPatchDTO {
    private Long cantidad;
    private EstadoReserva estadoReserva;

    public ReservaPatchDTO(Long cantidad, EstadoReserva estadoReserva) {
        this.cantidad = cantidad;
        this.estadoReserva = estadoReserva;
    }

    public Long getCantidad() {
        return cantidad;
    }

    public void setCantidad(Long cantidad) {
        this.cantidad = cantidad;
    }

    public EstadoReserva getEstadoReserva() {
        return estadoReserva;
    }

    public void setEstadoReserva(EstadoReserva estadoReserva) {
        this.estadoReserva = estadoReserva;
    }
}
