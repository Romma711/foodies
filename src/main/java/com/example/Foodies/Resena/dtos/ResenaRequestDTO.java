package com.example.Foodies.Resena.dtos;

import com.example.Foodies.Cliente.dtos.ClienteRequestDTO;
import com.example.Foodies.Restaurant.Dtos.RestaurantRequestDTO;

public class ResenaRequestDTO {
    private Long id;
    private String comentario;
    private int calificacion;
    private Long clienteId;
    private Long restaurantId;



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

    public Long getClienteId() {
        return clienteId;
    }

    public void setClienteId(Long clienteId) {
        this.clienteId = clienteId;
    }

    public Long getRestaurantId() {
        return restaurantId;
    }

    public void setRestaurantId(Long restaurantId) {
        this.restaurantId = restaurantId;
    }
}
