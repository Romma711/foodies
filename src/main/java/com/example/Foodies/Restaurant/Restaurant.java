package com.example.Foodies.Restaurant;

import com.example.Foodies.Carta.Carta;
import com.example.Foodies.EspecialidadDeComida.EspecialidadDeComida;
import com.example.Foodies.Resena.Resena;
import com.example.Foodies.Reserva.Reserva;
import com.example.Foodies.Usuario.Usuario;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.util.ArrayList;
import java.util.List;

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


     @OneToOne(mappedBy = "restaurant")
     private Carta carta;

     @ManyToOne
     @JoinColumn(name = "especialidad_id", nullable = false)
     private EspecialidadDeComida especialidad;

     @OneToOne
     @JoinColumn(name = "usuario_id",nullable = false)
     private Usuario usuario;

     @OneToMany(mappedBy = "Restaurants")
     private List<Reserva> reserva = new ArrayList<>();

     @OneToMany(mappedBy = "Restaurants")
     private List<Resena> resenas = new ArrayList<>();


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Integer getCupoMaximo() {
        return cupoMaximo;
    }

    public void setCupoMaximo(Integer cupoMaximo) {
        this.cupoMaximo = cupoMaximo;
    }

    public String getUbicacion() {
        return ubicacion;
    }

    public void setUbicacion(String ubicacion) {
        this.ubicacion = ubicacion;
    }

    public Carta getCarta() {
        return carta;
    }

    public void setCarta(Carta carta) {
        this.carta = carta;
    }

    public EspecialidadDeComida getEspecialidad() {
        return especialidad;
    }

    public void setEspecialidad(EspecialidadDeComida especialidad) {
        this.especialidad = especialidad;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public List<Reserva> getReserva() {
        return reserva;
    }

    public void setReserva(List<Reserva> reserva) {
        this.reserva = reserva;
    }

    public List<Resena> getResenas() {
        return resenas;
    }

    public void setResenas(List<Resena> resenas) {
        this.resenas = resenas;
    }
}
