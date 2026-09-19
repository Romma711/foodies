package com.example.Foodies.Resena.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResenaRequestDTO {
    private Long id;
    private String comentario;
    private int calificacion;
    private Long usuarioId;
    private Long restaurantId;
}