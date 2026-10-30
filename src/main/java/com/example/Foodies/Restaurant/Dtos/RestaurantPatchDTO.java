package com.example.Foodies.Restaurant.Dtos;

import com.example.Foodies.Enums.EspecialidadDeComida;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantPatchDTO {

    private String nombre;
    private Integer cupoMaximo;
    /** 0-100. Null = no cambiar; 0 = apagar la auto-aceptación. */
    private Integer porcentajeAutoAceptacion;
    private String ubicacion;
    private EspecialidadDeComida especialidad;
}