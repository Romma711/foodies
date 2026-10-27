package com.example.Foodies.ADMIN;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** End-to-end check of the metrics endpoint through the real JWT security chain. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AdminControllerTest {

    private static final String URL = "/api/admin/metricas";

    @Autowired
    private TestRestTemplate rest;

    private String login(String email, String password) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<Map> resp = rest.postForEntity(
                "/api/auth/login",
                new HttpEntity<>(Map.of("email", email, "password", password), headers),
                Map.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        return ((String) resp.getBody().get("Token")).substring("Bearer ".length());
    }

    private ResponseEntity<Map> get(String token) {
        HttpHeaders headers = new HttpHeaders();
        if (token != null) {
            headers.setBearerAuth(token);
        }
        return rest.exchange(URL, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
    }

    @Test
    void elAdminObtieneLasMetricas() {
        ResponseEntity<Map> resp = get(login("admin@foodies.com", "admin123"));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).containsKeys(
                "periodo",
                "topRestaurantesMejorCalificados",
                "topRestaurantesConMasReservas",
                "topUsuariosConMasResenas",
                "usuariosRegistradosEsteMes");
    }

    @Test
    void unClienteRecibe403() {
        rest.postForEntity("/api/auth/register/cliente", Map.of(
                "nombre", "Metricas",
                "apellido", "Cliente",
                "email", "metricas.cliente@test.com",
                "password", "secret1",
                "telefono", "1155550000"), Map.class);

        ResponseEntity<Map> resp = get(login("metricas.cliente@test.com", "secret1"));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void sinTokenRecibe401() {
        assertThat(get(null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void elAdminObtieneLasResenasRecientes() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(login("admin@foodies.com", "admin123"));

        ResponseEntity<java.util.List> resp = rest.exchange(
                "/api/admin/resenas?limite=5", HttpMethod.GET, new HttpEntity<>(headers), java.util.List.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();
    }

    @Test
    void unClienteNoPuedeVerLaModeracion() {
        rest.postForEntity("/api/auth/register/cliente", Map.of(
                "nombre", "Moderacion",
                "apellido", "Cliente",
                "email", "moderacion.cliente@test.com",
                "password", "secret1",
                "telefono", "1155551111"), Map.class);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(login("moderacion.cliente@test.com", "secret1"));

        ResponseEntity<String> resp = rest.exchange(
                "/api/admin/resenas", HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
