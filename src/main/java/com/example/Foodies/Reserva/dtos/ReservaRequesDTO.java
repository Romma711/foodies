package com.example.Foodies.Reserva.dtos;

import com.example.Foodies.Cliente.dtos.ClienteRequestDTO;
import com.example.Foodies.Enums.EstadoReserva;
import com.example.Foodies.Restaurant.Dtos.RestaurantRequestDTO;

import java.sql.Time;
import java.time.LocalTime;

public class ReservaRequesDTO {
    private Integer cantidad;
    private EstadoReserva estadoReserva;
    private String fechaReserva;
    private String horarioLlegada;
    private Long idCliente;
    private Long idRestaurant;

    public ReservaRequesDTO(Integer cantidad, EstadoReserva estadoReserva, String fechaReserva, String horarioLlegada, Long idCliente, Long idRestaurant) {
        this.cantidad = cantidad;
        this.estadoReserva = estadoReserva;
        this.fechaReserva = fechaReserva;
        this.horarioLlegada = horarioLlegada;
        this.idCliente = idCliente;
        this.idRestaurant = idRestaurant;
    }

    public String getHorarioLlegada() {
        return horarioLlegada;
    }

    public void setHorarioLlegada(String horarioLlegada) {
        this.horarioLlegada = horarioLlegada;
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

    public Long getIdCliente() {
        return idCliente;
    }

    public void setIdCliente(Long idCliente) {
        this.idCliente = idCliente;
    }

    public Long getIdRestaurant() {
        return idRestaurant;
    }

    public void setIdRestaurant(Long idRestaurant) {
        this.idRestaurant = idRestaurant;
    }

    public String getFechaReserva() {
        return fechaReserva;
    }

    public void setFechaReserva(String fechaReserva) {
        this.fechaReserva = fechaReserva;
    }
}
