package com.example.Foodies.Cliente;

import com.example.Foodies.Resena.Resena;
import com.example.Foodies.Reserva.Reserva;
import com.example.Foodies.Usuario.Usuario;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cliente")
public class Cliente {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String nombre;

    @NotBlank
    @Column(nullable = false)
    private String apellido;

    @OneToOne(orphanRemoval = true, cascade = CascadeType.ALL)
    @JsonManagedReference
    private Usuario usuario;

     @OneToMany(mappedBy = "cliente")
    private List<Reserva> reserva = new ArrayList<>();

     @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL)
    private List<Resena> resenas = new ArrayList<>();

    public Cliente(String nombre, String apellido, Usuario usuario) {
        this.nombre = nombre;
        this.apellido = apellido;
        this.usuario = usuario;
    }

    public Cliente() {
    }

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

    public String getApellido() {
        return apellido;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }
}
