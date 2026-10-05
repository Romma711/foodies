package com.example.Foodies.Config;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

/**
 * Falla al arrancar si no esta configurada la clave de firma del JWT,
 * en lugar de descubrirlo recien en el primer login.
 */
@Component
public class JwtSecretConfig {

    @PostConstruct
    public void validarSecret() {
        JwtUtil.verificarConfiguracion();
    }
}
