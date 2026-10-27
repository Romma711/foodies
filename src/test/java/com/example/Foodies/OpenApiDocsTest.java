package com.example.Foodies;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifica que la documentación OpenAPI sea pública y describa los endpoints de administración
 * con el esquema de autenticación JWT.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OpenApiDocsTest {

    @Autowired
    private TestRestTemplate rest;

    @Test
    void laEspecificacionEsPublicaEIncluyeAdminYJwt() {
        ResponseEntity<String> respuesta = rest.getForEntity("/v3/api-docs", String.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody())
                .contains("Foodies API")
                .contains("/api/admin/metricas")
                .contains("bearerAuth");
    }

    @Test
    void swaggerUiEsPublica() {
        ResponseEntity<String> respuesta = rest.getForEntity("/swagger-ui/index.html", String.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
