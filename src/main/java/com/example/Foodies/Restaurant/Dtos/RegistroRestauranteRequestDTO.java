package com.example.Foodies.Restaurant.Dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegistroRestauranteRequestDTO {
    private String email;
    private String password;
    private String nombreRestaurante;
    private String direccion;
    private String telefono;
    private String especialidadDeComida;
    private Integer cupoMaximo;
}