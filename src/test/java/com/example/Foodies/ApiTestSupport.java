package com.example.Foodies;

import com.example.Foodies.Restaurant.Restaurant;
import com.example.Foodies.Restaurant.RestaurantRepository;
import com.example.Foodies.Usuario.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Map;

/**
 * Helpers compartidos por los tests de punta a punta: todos los tests hablan con el
 * servidor real que levanta Spring Boot y la base H2 en memoria.
 *
 * Ojo con los HttpEntity de Spring:
 *  - new HttpEntity<>(headers)        -> solo headers (GET/DELETE)
 *  - new HttpEntity<>(body, headers)  -> body + headers (POST/PATCH)
 */
@SuppressWarnings({"unchecked", "rawtypes"})
abstract class ApiTestSupport extends TestBase {

    @Autowired
    protected TestRestTemplate rest;

    @Autowired
    protected RestaurantRepository restaurantRepo;

    @Autowired
    protected UsuarioRepository usuarioRepo;

    protected String login(String email, String password) {
        ResponseEntity<Map> resp = rest.postForEntity("/api/auth/login",
                new HttpEntity<>(Map.of("email", email, "password", password)), Map.class);
        if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null
                || resp.getBody().get("Token") == null) {
            return null;
        }
        return resp.getBody().get("Token").toString().substring("Bearer ".length());
    }

    protected String adminToken() {
        String token = login("admin@foodies.com", "admin123");
        if (token == null) {
            throw new IllegalStateException("No se pudo loguear el admin");
        }
        return token;
    }

    protected HttpHeaders auth(String token, boolean json) {
        HttpHeaders headers = new HttpHeaders();
        if (token != null) {
            headers.setBearerAuth(token);
        }
        if (json) {
            headers.setContentType(MediaType.APPLICATION_JSON);
        }
        return headers;
    }

    protected Long registrarCliente(String email, String nombre) {
        ResponseEntity<Map> resp = rest.postForEntity("/api/auth/register/cliente",
                new HttpEntity<>(Map.of(
                        "nombre", nombre, "apellido", "Apellido",
                        "email", email, "password", "secret1", "telefono", "111")),
                Map.class);
        if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null
                || resp.getBody().get("id") == null) {
            throw new IllegalStateException("No se pudo registrar el cliente " + email + " -> " + resp);
        }
        return Long.valueOf(resp.getBody().get("id").toString());
    }

    /** Registra un restaurante y lo deja aprobado (si aprobar) devolviendo su id. */
    protected Long registrarResto(String email, String nombre, int cupo, String especialidad, boolean aprobar) {
        ResponseEntity<Map> reg = rest.postForEntity("/api/auth/register/restaurante",
                new HttpEntity<>(Map.of(
                        "email", email, "password", "secret1",
                        "nombreRestaurante", nombre, "direccion", "Calle 10",
                        "telefono", "222", "especialidadDeComida", especialidad, "cupoMaximo", cupo)),
                Map.class);
        if (!reg.getStatusCode().is2xxSuccessful() || reg.getBody() == null
                || reg.getBody().get("id") == null) {
            throw new IllegalStateException("No se pudo registrar el restaurante " + email + " -> " + reg);
        }
        if (aprobar) {
            rest.exchange("/api/admin/approved/{id}", HttpMethod.PUT,
                    new HttpEntity<>(auth(adminToken(), false)), String.class,
                    Long.valueOf(reg.getBody().get("id").toString()));
        }
        return idResto(nombre);
    }

    protected Long idResto(String nombre) {
        return restaurantRepo.findAll().stream()
                .filter(r -> r.getNombre().equals(nombre))
                .map(Restaurant::getId)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No existe el restaurante " + nombre));
    }

    protected Long idUsuario(String email) {
        return usuarioRepo.findByEmail(email).orElseThrow(
                () -> new IllegalStateException("No existe el usuario " + email)).getId();
    }

    protected String fecha(int dias) {
        return LocalDate.now().plusDays(dias).format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    protected ResponseEntity<String> crearReserva(String token, Long usuarioId, Long restauranteId,
                                                  int cantidad, String dia, String hora) {
        return rest.exchange("/api/reservas", HttpMethod.POST,
                new HttpEntity<>(Map.of(
                        "cantidad", cantidad, "fechaReserva", dia, "horarioLlegada", hora,
                        "idUsuario", usuarioId, "idRestaurant", restauranteId),
                        auth(token, true)),
                String.class);
    }

    /** Devuelve el payload del JWT en crudo para poder mirar roles y sub. */
    protected String payloadDelToken(String token) {
        try {
            return new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]));
        } catch (Exception e) {
            return "";
        }
    }

    protected ResponseEntity<String> get(String token, String url, Object... uriVars) {
        return rest.exchange(url, HttpMethod.GET, new HttpEntity<>(auth(token, false)), String.class, uriVars);
    }

    /** GET que devuelve una Page ya parseada: {"content": [...], "totalElements": n, ...} */
    protected Map<String, Object> getPage(String token, String url, Object... uriVars) {
        ResponseEntity<Map> resp = rest.exchange(url, HttpMethod.GET,
                new HttpEntity<>(auth(token, false)), Map.class, uriVars);
        return resp.getBody();
    }

    protected Long idDe(Map<String, Object> body) {
        return Long.valueOf(body.get("id").toString());
    }
}
