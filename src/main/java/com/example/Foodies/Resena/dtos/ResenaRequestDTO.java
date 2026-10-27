package com.example.Foodies.Resena.dtos;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResenaRequestDTO {

    @NotBlank(message = "El comentario es obligatorio")
    @Size(min = 10, max = 255, message = "El comentario debe tener entre 10 y 255 caracteres")
    private String comentario;

    @Min(value = 1, message = "La calificación mínima es 1")
    @Max(value = 5, message = "La calificación máxima es 5")
    private int calificacion;

    @NotNull(message = "El restaurante es obligatorio")
    private Long restaurantId;
}
