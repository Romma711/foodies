package com.example.Foodies;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CORS: los origenes permitidos salen de CORS_ALLOWED_ORIGINS (la define TestBase).
 * Sin esa variable no hay CORS, que es el comportamiento por defecto.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CorsTest extends ApiTestSupport {

    private ResponseEntity<String> preflight(String origen, String metodo, String ruta) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Origin", origen);
        headers.set("Access-Control-Request-Method", metodo);
        headers.set("Access-Control-Request-Headers", "authorization,content-type");
        return rest.exchange(ruta, HttpMethod.OPTIONS, new HttpEntity<>(headers), String.class);
    }

    @Test
    void origenPermitidoPasaElPreflight() {
        ResponseEntity<String> r = preflight("http://localhost:3000", "POST", "/api/auth/login");
        assertThat(r.getHeaders().getAccessControlAllowOrigin()).isEqualTo("http://localhost:3000");
        assertThat(r.getHeaders().getAccessControlAllowCredentials()).isTrue();

        ResponseEntity<String> r2 = preflight("http://localhost:5173", "GET", "/api/restaurantes");
        assertThat(r2.getHeaders().getAccessControlAllowOrigin()).isEqualTo("http://localhost:5173");
    }

    @Test
    void origenNoPermitidoNoRecibePermiso() {
        ResponseEntity<String> r = preflight("http://sitio-malicioso.com", "POST", "/api/auth/login");
        assertThat(r.getHeaders().getAccessControlAllowOrigin()).isNull();
    }

    @Test
    void peticionNormalConOriginPermitidoDevuelveElHeader() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Origin", "http://localhost:3000");

        ResponseEntity<String> r = rest.exchange("/api/restaurantes", HttpMethod.GET,
                new HttpEntity<>(headers), String.class);

        assertThat(r.getStatusCode().is2xxSuccessful() || r.getStatusCode().value() == 204).isTrue();
        assertThat(r.getHeaders().getAccessControlAllowOrigin()).isEqualTo("http://localhost:3000");
    }
}