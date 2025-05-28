package com.example.Foodies.Restaurant;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
@Table(name = "Restaurants")
@Entity
public class Restaurant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @NotBlank(message = "Nombre de restaurant es obliglatorio")
    private String nombre;

    @Column(nullable = false)
    @Min(value = 1,message = "El cupo debe ser mayor a 1")
    private Integer cupoMaximo;


    @Column(nullable = false)
    @NotBlank(message = "La ubicacion es obligatoria")
    private String ubicacion;
     /*
     @OneToMany(mappedBy = "Restaurants")
    private List<Reserva> reserva = new ArrayList<>();

    @OneToMany(mappedBy = "Restaurants")
    private List<Resena> resenas = new ArrayList<>();
     */


}
