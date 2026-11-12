package com.example.Foodies.Usuario.dtos;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioPatchDTO {
    @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
    private String nombre;
    @Size(max = 100, message = "El apellido no puede superar los 100 caracteres")
    private String apellido;
    @Size(max = 30, message = "El teléfono no puede superar los 30 caracteres")
    private String telefono;
}