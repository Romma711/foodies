package com.example.Foodies.Usuario;

import com.example.Foodies.Cliente.Cliente;
import com.example.Foodies.Restaurant.Restaurant;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "usuario")
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String email;

    @NotBlank
    private String password;

    @NotBlank
    private String telefono;
   /*
    @OneToOne(mappedBy = "usuario")
    private Restaurant restaurant;

    @OneToOne(mappedBy = "usuairio")
    private Cliente cliente;

   */
    public Usuario(String email, String password, String telefono) {
        this.email = email;
        this.password = password;
        this.telefono = telefono;
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
