package com.example.Foodies.Restaurant.Dtos;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
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
}