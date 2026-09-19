package com.example.Foodies.Usuario.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioPatchDTO {
    private String nombre;
    private String apellido;
    private String telefono;
}