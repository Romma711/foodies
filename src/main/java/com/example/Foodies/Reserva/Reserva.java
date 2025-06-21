package com.example.Foodies.Reserva;

import com.example.Foodies.Cliente.Cliente;
import com.example.Foodies.Cliente.dtos.ClienteRequestDTO;
import com.example.Foodies.Enums.EstadoReserva;
import com.example.Foodies.Restaurant.Dtos.RestaurantRequestDTO;
import com.example.Foodies.Restaurant.Restaurant;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Date;

@Entity
@Table(name = "reserva")
public class Reserva {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @NotNull
    private Integer cantidad;

    @NotNull
    private Date fechaReserva;

    @NotNull
    private LocalTime horariollegada;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private EstadoReserva estadoReserva;

    @ManyToOne
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;
    @ManyToOne
    @JoinColumn(name = "restaurant_id")
    private Restaurant restaurant;

    public Reserva(Long id, Integer cantidad, Date fechaReserva, LocalTime horariollegada, EstadoReserva estadoReserva, Cliente cliente, Restaurant restaurant) {
        this.id = id;
        this.cantidad = cantidad;
        this.fechaReserva = fechaReserva;
        this.horariollegada = horariollegada;
        this.estadoReserva = estadoReserva;
        this.cliente = cliente;
        this.restaurant = restaurant;
    }

    public Reserva(Integer cantidad, Date fechaReserva, LocalTime horariollegada, EstadoReserva estadoReserva, Cliente cliente, Restaurant restaurant) {
        this.cantidad = cantidad;
        this.fechaReserva = fechaReserva;
        this.horariollegada = horariollegada;
        this.estadoReserva = estadoReserva;
        this.cliente = cliente;
        this.restaurant = restaurant;
    }

    public Reserva() {
    }

    public Long getId() {return id;}
    public void setId(Long id) {this.id = id;}
    public @NotNull Integer getCantidad() {return cantidad;}
    public void setCantidad(@NotNull Integer cantidad) {this.cantidad = cantidad;}
    public @NotNull LocalTime getHorariollegada() {return horariollegada;}
    public void setHorariollegada(@NotNull LocalTime horariollegada) {this.horariollegada = horariollegada;}
    public EstadoReserva getEstadoReserva() {return estadoReserva;}
    public void setEstadoReserva(EstadoReserva estadoReserva) {this.estadoReserva = estadoReserva;}
    public Cliente getCliente() {return cliente;}
    public void setCliente(Cliente cliente) {this.cliente = cliente;}
    public Restaurant getRestaurant() {return restaurant;}
    public void setRestaurant(Restaurant restaurant) {this.restaurant = restaurant;}
    public Date getFechaReserva() {
        return fechaReserva;
    }
    public void setFechaReserva(Date fechaReserva) {
        this.fechaReserva = fechaReserva;
    }
}
