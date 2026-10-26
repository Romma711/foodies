package com.example.Foodies.Config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de la documentación interactiva de la API (springdoc-openapi).
 *
 * <p>Expone la especificación OpenAPI en {@code /v3/api-docs} y la interfaz Swagger UI en
 * {@code /swagger-ui.html}. Ambas rutas son públicas en {@link SecurityConfig}; los endpoints
 * protegidos se prueban desde Swagger UI pegando el token JWT en el botón "Authorize".</p>
 */
@Configuration
public class OpenApiConfig {

    /** Nombre del esquema de seguridad que referencian todos los endpoints. */
    static final String ESQUEMA_JWT = "bearerAuth";

    /**
     * Define los metadatos de la API y el esquema de autenticación Bearer JWT, aplicado de forma
     * global para que Swagger UI envíe el header {@code Authorization} en cada request.
     *
     * @return la definición OpenAPI que springdoc combina con los controladores escaneados
     */
    @Bean
    public OpenAPI foodiesOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Foodies API")
                        .version("v1")
                        .description("API REST de reservas, reseñas y administración de restaurantes."))
                .components(new Components().addSecuritySchemes(ESQUEMA_JWT, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA_JWT));
    }
}
