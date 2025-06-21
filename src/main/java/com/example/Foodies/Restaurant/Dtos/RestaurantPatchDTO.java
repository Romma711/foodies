package com.example.Foodies.Restaurant.Dtos;

import com.example.Foodies.Enums.EspecialidadDeComida;

public class RestaurantPatchDTO {

    private String nombre;
    private Integer cupoMaximo;
    private String ubicacion;
    private EspecialidadDeComida especialidad;

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

    public EspecialidadDeComida getEspecialidad() {
        return especialidad;
    }

    public void setEspecialidad(EspecialidadDeComida especialidad) {
        this.especialidad = especialidad;
    }
}
