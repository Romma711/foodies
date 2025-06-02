package com.example.Foodies.Restaurant.Dtos;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class RestaurantRequestDTO {

    @NotBlank(message = "Nombre de restaurant es obligatorio")
    private String nombre;

    @Min(value = 1, message = "El cupo debe ser mayor a 1")
    private Integer cupoMaximo;

    @NotBlank(message = "La ubicación es obligatoria")
    private String ubicacion;

    @NotNull
    private Long usuarioId;

    @NotNull
    private Long EspecialidadId;

    public Long getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
    }

    public Long getEspecialidadId() {
        return EspecialidadId;
    }

    public void setEspecialidadId(Long especialidadId) {
        EspecialidadId = especialidadId;
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
}
