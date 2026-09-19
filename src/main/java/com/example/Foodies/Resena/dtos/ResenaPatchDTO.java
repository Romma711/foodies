package com.example.Foodies.Resena.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResenaPatchDTO {
   private Long id;
    private String comentario;
    private Integer calificacion;
}