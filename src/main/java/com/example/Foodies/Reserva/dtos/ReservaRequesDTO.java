package com.example.Foodies.Reserva.dtos;

import com.example.Foodies.Cliente.dtos.ClienteRequestDTO;
import com.example.Foodies.Enums.EstadoReserva;
import com.example.Foodies.Restaurant.Dtos.RestaurantRequestDTO;

public class ReservaRequesDTO {
    private Long id;
    private Long cantidad;
    private EstadoReserva estadoReserva;
    private ClienteRequestDTO cliente;
    private RestaurantRequestDTO restaurant;

    public ReservaRequesDTO(Long id, Long cantidad, EstadoReserva estadoReserva, ClienteRequestDTO cliente, RestaurantRequestDTO restaurant) {
        this.id = id;
        this.cantidad = cantidad;
        this.estadoReserva = estadoReserva;
        this.cliente = cliente;
        this.restaurant = restaurant;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public ClienteRequestDTO getCliente() {
        return cliente;
    }

    public void setCliente(ClienteRequestDTO cliente) {
        this.cliente = cliente;
    }

    public RestaurantRequestDTO getRestaurant() {
        return restaurant;
    }

    public void setRestaurant(RestaurantRequestDTO restaurant) {
        this.restaurant = restaurant;
    }
}
