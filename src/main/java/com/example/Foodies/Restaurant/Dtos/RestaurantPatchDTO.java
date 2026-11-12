package com.example.Foodies.Restaurant.Dtos;

import com.example.Foodies.Enums.EspecialidadDeComida;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantPatchDTO {

    public static final String CAPACIDAD_POR_ESPACIOS =
            "La capacidad se configura por espacio desde la sección Espacios";

    private String nombre;
    /** Ya no se edita: la capacidad total se deriva de los espacios. Si llega, se rechaza con 400. */
    private Integer cupoMaximo;
    /** 0-100. Null = no cambiar; 0 = apagar la auto-aceptación. */
    private Integer porcentajeAutoAceptacion;
    private String ubicacion;
    private EspecialidadDeComida especialidad;
}