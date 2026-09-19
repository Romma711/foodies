package com.example.Foodies.Restaurant;

import com.example.Foodies.Carta.Carta;
import com.example.Foodies.Enums.EspecialidadDeComida;
import com.example.Foodies.Resena.Resena;
import com.example.Foodies.Reserva.Reserva;
import com.example.Foodies.Usuario.Usuario;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Table(name = "Restaurants")
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
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

    @NotNull
    private boolean aprobado = false;



    @OneToOne(mappedBy = "restaurant", cascade = CascadeType.ALL, orphanRemoval = true)
    private Carta carta;

     @Enumerated(EnumType.STRING)
     private EspecialidadDeComida especialidad;

     @OneToOne(orphanRemoval = true, cascade = CascadeType.ALL)
     @JoinColumn(name = "usuario_id",nullable = false)
     private Usuario usuario;

     @OneToMany(mappedBy = "restaurant", orphanRemoval = true, cascade = CascadeType.ALL)
     private List<Reserva> reserva = new ArrayList<>();

     @OneToMany(mappedBy = "restaurant", orphanRemoval = true, cascade = CascadeType.ALL)
     private List<Resena> resenas = new ArrayList<>();

}