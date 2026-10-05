package com.example.Foodies.Config;

import com.fasterxml.jackson.databind.MapperFeature;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Acepta los enums sin distinguir mayúsculas: "cafe", "CAFE" y "Cafe" son lo mismo.
 * Antes el servicio hacia un toUpperCase() a mano y hoy, al tipar la especialidad
 * como enum, un "cafe" en minuscula devolvia 400.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        return builder -> builder.featuresToEnable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS);
    }
}