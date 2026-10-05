package com.example.Foodies.Config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * CORS para que un frontend servido en otro origen pueda consumir la API.
 *
 * Los origenes permitidos vienen de la variable de entorno CORS_ALLOWED_ORIGINS
 * (separados por coma). Si no esta definida no se habilita ninguno: la API sigue
 * funcionando igual para Postman, curl o apps moviles (quien no manda cabecera
 * Origin no se ve afectado).
 *
 *   export CORS_ALLOWED_ORIGINS=http://localhost:3000,https://foodies.com
 *
 * Se expone como CorsConfigurationSource (y no solo con addCorsMappings) para que
 * Spring Security resuelva tambien los preflight OPTIONS de los endpoints protegidos.
 */
@Configuration
public class CorsConfig {

    @Value("${CORS_ALLOWED_ORIGINS:}")
    private String origenesPermitidos;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        List<String> origenes = Arrays.stream(origenesPermitidos.split(","))
                .map(String::trim)
                .filter(o -> !o.isEmpty())
                .toList();

        config.setAllowedOrigins(origenes);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}