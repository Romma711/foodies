package com.example.Foodies.Reserva;

import com.example.Foodies.Cliente.Cliente;
import com.example.Foodies.Cliente.dtos.ClienteRequestDTO;
import com.example.Foodies.Enums.EstadoReserva;
import com.example.Foodies.Restaurant.Dtos.RestaurantRequestDTO;
import com.example.Foodies.Restaurant.Restaurant;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

@Entity
@Table(name = "reserva")
public class Reserva {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @NotNull
    private Long cantidad;

    @NotNull
    private LocalTime horariollegada;

    @NotNull
    private LocalTime horariofin;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private EstadoReserva estadoReserva;

    @ManyToOne
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;
    @ManyToOne
    @JoinColumn(name = "restaurant_id")
    private Restaurant restaurant;

    public Reserva(Long cantidad, EstadoReserva estadoReserva, ClienteRequestDTO cliente, RestaurantRequestDTO restaurant) {
    }

    public Long getId() {return id;}
    public void setId(Long id) {this.id = id;}
    public @NotNull Long getCantidad() {return cantidad;}
    public void setCantidad(@NotNull Long cantidad) {this.cantidad = cantidad;}
    public @NotNull LocalTime getHorariollegada() {return horariollegada;}
    public void setHorariollegada(@NotNull LocalTime horariollegada) {this.horariollegada = horariollegada;}
    public @NotNull LocalTime getHorariofin() {return horariofin;}
    public void setHorariofin(@NotNull LocalTime horariofin) {this.horariofin = horariofin;}
    public EstadoReserva getEstadoReserva() {return estadoReserva;}
    public void setEstadoReserva(EstadoReserva estadoReserva) {this.estadoReserva = estadoReserva;}
    public Cliente getCliente() {return cliente;}
    public void setCliente(Cliente cliente) {this.cliente = cliente;}
    public Restaurant getRestaurant() {return restaurant;}
    public void setRestaurant(Restaurant restaurant) {this.restaurant = restaurant;}
}
