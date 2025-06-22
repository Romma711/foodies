package com.example.Foodies.Usuario;

import com.example.Foodies.Cliente.Cliente;
import com.example.Foodies.Enums.Role;
import com.example.Foodies.Restaurant.Restaurant;
import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "usuario")
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Email(message = "El email debe ser valido")
    @Column(nullable = false)
    private String email;

    @NotBlank
    private String password;

    @NotBlank
    @Column(nullable = false)
    private String telefono;

    @Enumerated(EnumType.STRING)
    private Role rol;

    @OneToOne(mappedBy = "usuario")
    private Restaurant restaurant;

    @OneToOne(mappedBy = "usuario")
    @JsonBackReference
    private Cliente cliente;

    public Usuario(String email, String password, String telefono, Role rol) {
        this.email = email;
        this.password = password;
        this.telefono = telefono;
        this.rol = rol;
    }
    public Usuario(){}


    public Role getRol() {
        return rol;
    }
    public void setRol(Role role) {
        this.rol = role;
    }
    public Restaurant getRestaurant() {
        return restaurant;
    }
    public void setRestaurant(Restaurant restaurant) {
        this.restaurant = restaurant;
    }
    public Cliente getCliente() {
        return cliente;
    }
    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    public String getTelefono() {
        return telefono;
    }
    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }
}
