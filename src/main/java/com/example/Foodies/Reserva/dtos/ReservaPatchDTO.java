package com.example.Foodies.Reserva.dtos;

import com.example.Foodies.Enums.EstadoReserva;

public class ReservaPatchDTO {
    private Integer cantidad;
    private EstadoReserva estadoReserva;

    public ReservaPatchDTO(Integer cantidad, EstadoReserva estadoReserva) {
        this.cantidad = cantidad;
        this.estadoReserva = estadoReserva;
    }

    public Integer getCantidad() {
        return cantidad;
    }

    public void setCantidad(Integer cantidad) {
        this.cantidad = cantidad;
    }

    public EstadoReserva getEstadoReserva() {
        return estadoReserva;
    }

    public void setEstadoReserva(EstadoReserva estadoReserva) {
        this.estadoReserva = estadoReserva;
    }
}
