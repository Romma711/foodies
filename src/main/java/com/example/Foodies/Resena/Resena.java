package com.example.Foodies.Resena;

import com.example.Foodies.Cliente.Cliente;
import com.example.Foodies.Restaurant.Restaurant;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "resena")
public class Resena {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @NotBlank(message = "No se permite añadir reseña sin comentario")
    private String comentario;

    @Column(nullable = false)
    @Min(value = 1, message = "La calificación mínima es 1")
    @Max(value = 5, message = "La calificación máxima es 5")
    private int calificacion;

    @ManyToOne
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;
    @ManyToOne
    @JoinColumn(name = "restaurant_id")
    private Restaurant restaurant;

    public Resena(Long id, String comentario, int calificacion, Cliente cliente, Restaurant restaurant) {
        this.id = id;
        this.comentario = comentario;
        this.calificacion = calificacion;
        this.cliente = cliente;
        this.restaurant = restaurant;
    }

    public Resena (){}
    public Long getId() {return id;}
    public void setId(Long id) {this.id = id;}
    public @NotBlank(message = "No se permite añadir reseña sin comentario") String getComentario() {return comentario;}
    public void setComentario(@NotBlank(message = "No se permite añadir reseña sin comentario") String comentario) {this.comentario = comentario;}
    public int getCalificacion() {return calificacion;}
    public void setCalificacion(int calificacion) {this.calificacion = calificacion;}
    public Cliente getCliente() {return cliente;}
    public void setCliente(Cliente cliente) {this.cliente = cliente;}
    public Restaurant getRestaurant() {return restaurant;}
    public void setRestaurant(Restaurant restaurant) {this.restaurant = restaurant;}
}
