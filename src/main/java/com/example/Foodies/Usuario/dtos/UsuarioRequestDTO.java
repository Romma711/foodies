package com.example.Foodies.Usuario.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class UsuarioRequestDTO {
    @NotBlank
    @Email
    private String email;
    @NotBlank
    private String password;

}
