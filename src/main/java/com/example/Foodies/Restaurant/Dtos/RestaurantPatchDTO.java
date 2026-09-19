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
    private String ubicacion;
    private EspecialidadDeComida especialidad;
}