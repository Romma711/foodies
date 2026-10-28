package com.example.Foodies.Moderacion;

import com.example.Foodies.Restaurant.Restaurant;
import com.example.Foodies.Restaurant.RestaurantRepository;
import com.example.Foodies.Usuario.UsuarioRepository;
import com.example.Foodies.Usuario.dtos.UsuarioDetailDTO;
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
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end checks of the Phase 3 entity management: listing clients and restaurants and the
 * logical delete toggle, including what a deactivated account or restaurant can no longer do.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EntidadesControllerTest {

    @Autowired
    private TestRestTemplate rest;
    @Autowired
    private RestaurantRepository restaurantRepo;
    @Autowired
    private UsuarioRepository usuarioRepo;

    @MockitoBean
    private JavaMailSender mailSender;

    private record Cliente(Long id, String email) {}

    @Test
    void listaLosClientesConSusStrikesYEstado() {
        Cliente cliente = registrarCliente();

        ResponseEntity<List> resp = send(HttpMethod.GET, "/api/admin/entidades/clientes", admin(), null, List.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<?, ?> fila = buscar(resp.getBody(), "email", cliente.email());
        assertThat(fila.get("id")).isEqualTo(cliente.id().intValue());
        assertThat(fila.get("strikes")).isEqualTo(0);
        assertThat(fila.get("activo")).isEqualTo(true);
        assertThat(resp.getBody()).noneSatisfy(item ->
                assertThat(((Map<?, ?>) item).get("email")).isEqualTo("admin@foodies.com"));
    }

    @Test
    void listaLosRestaurantesConEstadoYEmailDelEncargado() {
        String nombre = registrarRestaurante();

        ResponseEntity<List> resp = send(HttpMethod.GET, "/api/admin/entidades/restaurantes", admin(), null, List.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<?, ?> fila = buscar(resp.getBody(), "nombre", nombre);
        assertThat(fila.get("estado")).isEqualTo("PENDIENTE");
        assertThat(fila.get("activo")).isEqualTo(true);
        assertThat(fila.get("strikes")).isEqualTo(0);
        assertThat((String) fila.get("email")).startsWith("resto-");
    }

    @Test
    void unClienteDadoDeBajaNoPuedeLoguearseHastaQueLoReactivan() {
        Cliente cliente = registrarCliente();

        ResponseEntity<Map> baja = send(HttpMethod.PATCH, "/api/admin/usuarios/" + cliente.id() + "/activo", admin(),
                Map.of("activo", false), Map.class);
        assertThat(baja.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(baja.getBody()).containsEntry("activo", false);
        assertThat(usuarioRepo.findById(cliente.id()).orElseThrow().getActivo()).isFalse();

        ResponseEntity<Map> login = intentarLogin(cliente.email(), "secret1");
        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(login.getBody()).containsEntry("error", "cuenta dada de baja");

        send(HttpMethod.PATCH, "/api/admin/usuarios/" + cliente.id() + "/activo", admin(), Map.of("activo", true), Map.class);
        assertThat(intentarLogin(cliente.email(), "secret1").getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void unTokenEmitidoAntesDeLaBajaDejaDeServir() {
        Cliente cliente = registrarCliente();
        String token = login(cliente.email(), "secret1");
        assertThat(send(HttpMethod.GET, "/api/reservas/usuario/" + cliente.id(), token, null, String.class)
                .getStatusCode()).isNotEqualTo(HttpStatus.FORBIDDEN);

        send(HttpMethod.PATCH, "/api/admin/usuarios/" + cliente.id() + "/activo", admin(), Map.of("activo", false), Map.class);

        ResponseEntity<Map> resp = send(HttpMethod.GET, "/api/reservas/usuario/" + cliente.id(), token, null, Map.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(resp.getBody()).containsEntry("error", "cuenta dada de baja");
    }

    @Test
    void unaCredencialIncorrectaDeUnaCuentaActivaSigueRespondiendoComoAntes() {
        Cliente cliente = registrarCliente();

        ResponseEntity<Map> login = intentarLogin(cliente.email(), "otra-clave");

        assertThat(login.getStatusCode()).isNotEqualTo(HttpStatus.OK);
        assertThat(login.getBody()).doesNotContainEntry("error", "cuenta dada de baja");
    }

    @Test
    void unRestauranteDadoDeBajaDesapareceDelCatalogoPublico() {
        String nombre = registrarRestaurante();
        Long id = idRestaurante(nombre);
        send(HttpMethod.PUT, "/api/admin/restaurantes/" + id + "/aprobar", admin(), null, Map.class);
        assertThat(nombresPublicos()).contains(nombre);

        ResponseEntity<Map> baja = send(HttpMethod.PATCH, "/api/admin/restaurantes/" + id + "/activo", admin(),
                Map.of("activo", false), Map.class);
        assertThat(baja.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(baja.getBody()).containsEntry("activo", false);
        assertThat(nombresPublicos()).doesNotContain(nombre);

        send(HttpMethod.PATCH, "/api/admin/restaurantes/" + id + "/activo", admin(), Map.of("activo", true), Map.class);
        assertThat(nombresPublicos()).contains(nombre);
    }

    @Test
    void noSePuedeDarDeBajaAUnAdministrador() {
        Long adminId = usuarioRepo.findByEmail("admin@foodies.com").orElseThrow().getId();

        ResponseEntity<Map> resp = send(HttpMethod.PATCH, "/api/admin/usuarios/" + adminId + "/activo", admin(),
                Map.of("activo", false), Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(usuarioRepo.findById(adminId).orElseThrow().getActivo()).isTrue();
    }

    @Test
    void elToggleSinValorDevuelve400() {
        Cliente cliente = registrarCliente();

        ResponseEntity<Map> resp = send(HttpMethod.PATCH, "/api/admin/usuarios/" + cliente.id() + "/activo", admin(),
                Map.of(), Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(usuarioRepo.findById(cliente.id()).orElseThrow().getActivo()).isTrue();
    }

    @Test
    void unClienteNoPuedeGestionarEntidades() {
        Cliente cliente = registrarCliente();
        String token = login(cliente.email(), "secret1");

        assertThat(send(HttpMethod.GET, "/api/admin/entidades/clientes", token, null, String.class).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(send(HttpMethod.PATCH, "/api/admin/usuarios/" + cliente.id() + "/activo", token,
                Map.of("activo", false), String.class).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    private List<Object> nombresPublicos() {
        ResponseEntity<List> resp = rest.getForEntity("/api/restaurantes", List.class);
        if (resp.getBody() == null) {
            return List.of();
        }
        return ((List<?>) resp.getBody()).stream().map(item -> ((Map<?, ?>) item).get("nombre")).map(Object.class::cast).toList();
    }

    private Map<?, ?> buscar(List<?> filas, String campo, Object valor) {
        return filas.stream()
                .map(item -> (Map<?, ?>) item)
                .filter(fila -> valor.equals(fila.get(campo)))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No se encontró " + campo + "=" + valor));
    }

    private Long idRestaurante(String nombre) {
        return restaurantRepo.findAll().stream()
                .filter(r -> r.getNombre().equals(nombre))
                .map(Restaurant::getId)
                .findFirst()
                .orElseThrow();
    }

    private String registrarRestaurante() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String nombre = "Resto " + suffix;
        ResponseEntity<UsuarioDetailDTO> reg = rest.postForEntity("/api/auth/register/restaurante", Map.of(
                "email", "resto-" + suffix + "@test.com",
                "password", "secret1",
                "nombreRestaurante", nombre,
                "direccion", "Calle " + suffix,
                "telefono", "2230000000",
                "especialidadDeComida", "PASTAS",
                "cupoMaximo", 10), UsuarioDetailDTO.class);
        assertThat(reg.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return nombre;
    }

    private Cliente registrarCliente() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String email = "cliente-" + suffix + "@test.com";
        ResponseEntity<UsuarioDetailDTO> reg = rest.postForEntity("/api/auth/register/cliente", Map.of(
                "nombre", "Cliente",
                "apellido", suffix,
                "email", email,
                "password", "secret1",
                "telefono", "2231111111"), UsuarioDetailDTO.class);
        assertThat(reg.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return new Cliente(reg.getBody().id(), email);
    }

    private String admin() {
        return login("admin@foodies.com", "admin123");
    }

    private ResponseEntity<Map> intentarLogin(String email, String password) {
        return rest.postForEntity("/api/auth/login", Map.of("email", email, "password", password), Map.class);
    }

    private String login(String email, String password) {
        ResponseEntity<Map> resp = intentarLogin(email, password);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        return ((String) resp.getBody().get("Token")).substring("Bearer ".length());
    }

    private <T> ResponseEntity<T> send(HttpMethod method, String url, String token, Object body, Class<T> type) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return rest.exchange(url, method, new HttpEntity<>(body, headers), type);
    }
}
