package com.example.Foodies;

import com.example.Foodies.Restaurant.Restaurant;
import com.example.Foodies.Restaurant.RestaurantRepository;
import com.example.Foodies.Usuario.dtos.UsuarioDetailDTO;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** HTTP helpers shared by the ownership tests: register users, log in and call the API with a token. */
final class AutorizacionTestSupport {

    record Cliente(String token, Long id, String email) {}

    record Encargado(String token, Long restaurantId) {}

    private final TestRestTemplate rest;
    private final RestaurantRepository restaurantRepo;

    AutorizacionTestSupport(TestRestTemplate rest, RestaurantRepository restaurantRepo) {
        this.rest = rest;
        this.restaurantRepo = restaurantRepo;
    }

    String admin() {
        return login("admin@foodies.com", "admin123");
    }

    String login(String email, String password) {
        ResponseEntity<Map> resp = rest.postForEntity("/api/auth/login", Map.of("email", email, "password", password), Map.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        return ((String) resp.getBody().get("Token")).substring("Bearer ".length());
    }

    Cliente nuevoCliente() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String email = "cliente-" + suffix + "@test.com";
        ResponseEntity<UsuarioDetailDTO> reg = rest.postForEntity("/api/auth/register/cliente", Map.of(
                "nombre", "Cliente",
                "apellido", suffix,
                "email", email,
                "password", "secret1",
                "telefono", "2231111111"), UsuarioDetailDTO.class);
        assertThat(reg.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return new Cliente(login(email, "secret1"), reg.getBody().id(), email);
    }

    /** Approved manager of a new restaurant with capacity 10. */
    Encargado nuevoEncargado() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String email = "encargado-" + suffix + "@test.com";
        String nombre = "Resto " + suffix;
        ResponseEntity<UsuarioDetailDTO> reg = rest.postForEntity("/api/auth/register/restaurante",
                RegistroRestauranteMultipart.conFotos(Map.of(
                        "email", email,
                        "password", "secret1",
                        "nombreRestaurante", nombre,
                        "direccion", "Calle " + suffix,
                        "telefono", "2230000000",
                        "especialidadDeComida", "PASTAS",
                        "cupoMaximo", 10), 3), UsuarioDetailDTO.class);
        assertThat(reg.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        ResponseEntity<UsuarioDetailDTO> aprobado = rest.exchange("/api/admin/approved/{id}", HttpMethod.PUT,
                new HttpEntity<>(headers(admin())), UsuarioDetailDTO.class, reg.getBody().id());
        assertThat(aprobado.getStatusCode()).isEqualTo(HttpStatus.OK);
        Long restaurantId = restaurantRepo.findAll().stream()
                .filter(r -> r.getNombre().equals(nombre))
                .map(Restaurant::getId)
                .findFirst()
                .orElseThrow();
        return new Encargado(login(email, "secret1"), restaurantId);
    }

    ResponseEntity<Map> reservar(Cliente cliente, Long restaurantId, LocalDate fecha, int cantidad) {
        Map<String, Object> body = new HashMap<>();
        body.put("cantidad", cantidad);
        body.put("fechaReserva", fecha.toString());
        body.put("horarioLlegada", "20:00");
        body.put("idUsuario", cliente.id());
        body.put("idRestaurant", restaurantId);
        return send(HttpMethod.POST, "/api/reservas", cliente.token(), body, Map.class);
    }

    Long crearReserva(Cliente cliente, Long restaurantId, LocalDate fecha, int cantidad) {
        ResponseEntity<Map> resp = reservar(cliente, restaurantId, fecha, cantidad);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        return ((Number) resp.getBody().get("id")).longValue();
    }

    HttpStatusCode status(HttpMethod method, String path, String token, Object body) {
        return send(method, path, token, body, String.class).getStatusCode();
    }

    <T> ResponseEntity<T> send(HttpMethod method, String path, String token, Object body, Class<T> type) {
        HttpHeaders headers = headers(token);
        if (body != null) {
            headers.setContentType(MediaType.APPLICATION_JSON);
        }
        return rest.exchange(path, method, new HttpEntity<>(body, headers), type);
    }

    static HttpHeaders headers(String token) {
        HttpHeaders headers = new HttpHeaders();
        if (token != null) {
            headers.setBearerAuth(token);
        }
        return headers;
    }
}
