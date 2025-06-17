package com.example.Foodies.Resena.dtos;

import com.example.Foodies.Cliente.dtos.ClienteRequestDTO;
import com.example.Foodies.Restaurant.Dtos.RestaurantRequestDTO;

public class ResenaRequestDTO {
    private Long id;
    private String comentario;
    private int calificacion;
    private ClienteRequestDTO cliente;
    private RestaurantRequestDTO restaurant;

    public ResenaRequestDTO(Long id, String comentario, int calificacion, ClienteRequestDTO cliente, RestaurantRequestDTO restaurant) {
        this.id = id;
        this.comentario = comentario;
        this.calificacion = calificacion;
        this.cliente = cliente;
        this.restaurant = restaurant;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getComentario() {
        return comentario;
    }

    public void setComentario(String comentario) {
        this.comentario = comentario;
    }

    public int getCalificacion() {
        return calificacion;
    }

    public void setCalificacion(int calificacion) {
        this.calificacion = calificacion;
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
