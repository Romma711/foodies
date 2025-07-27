package com.example.Foodies.Restaurant.Dtos;

public class RegistroRestauranteRequestDTO {
    String email;
    String password;
    String nombreRestaurante;
    String direccion;
    String telefono;
    String especialidadDeComida;
    Integer cupoMaximo;

    public String getEspecialidadDeComida() {
        return especialidadDeComida;
    }

    public void setEspecialidadDeComida(String especialidadDeComida) {
        this.especialidadDeComida = especialidadDeComida;
    }

    public Integer getCupoMaximo() {
        return cupoMaximo;
    }

    public void setCupoMaximo(Integer cupoMaximo) {
        this.cupoMaximo = cupoMaximo;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
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

    public String getNombreRestaurante() {
        return nombreRestaurante;
    }

    public void setNombreRestaurante(String nombreRestaurante) {
        this.nombreRestaurante = nombreRestaurante;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }
}
