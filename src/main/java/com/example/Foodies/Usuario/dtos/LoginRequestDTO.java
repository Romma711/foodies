package com.example.Foodies.Usuario.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequestDTO {
    @NotBlank(message = "Tiene que ser un email valido")
    @Email(message = "El email debe ser valido")
    private String email;
    @NotBlank(message = "La contraseña no es valida")
    private String password;
}