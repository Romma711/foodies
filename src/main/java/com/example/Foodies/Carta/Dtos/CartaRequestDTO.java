package com.example.Foodies.Carta.Dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartaRequestDTO {
    private String nombreArchivo;
    private byte[] contenidoPdf;
    private Long restaurantId;
}