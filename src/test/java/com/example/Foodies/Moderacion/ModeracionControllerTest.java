package com.example.Foodies.Moderacion;

import com.example.Foodies.Enums.EstadoRestaurante;
import com.example.Foodies.Restaurant.Restaurant;
import com.example.Foodies.Restaurant.RestaurantRepository;
import com.example.Foodies.Usuario.UsuarioRepository;
import com.example.Foodies.Usuario.dtos.UsuarioDetailDTO;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * End-to-end checks of the Phase 3 moderation endpoints through the real JWT security chain.
 * The mail sender is mocked so no SMTP connection is attempted.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ModeracionControllerTest {

    @Autowired
    private TestRestTemplate rest;
    @Autowired
    private RestaurantRepository restaurantRepo;
    @Autowired
    private UsuarioRepository usuarioRepo;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private EstadoRestauranteBackfill backfill;

    @MockitoBean
    private JavaMailSender mailSender;

    private record Solicitud(Long usuarioId, Long restaurantId, String nombre, String email) {}

    @BeforeEach
    void limpiarMocks() {
        reset(mailSender);
    }

    @Test
    void unRestauranteNuevoQuedaPendienteConValoresPorDefecto() {
        Solicitud solicitud = registrarRestaurante();

        Restaurant restaurant = restaurantRepo.findById(solicitud.restaurantId()).orElseThrow();
        assertThat(restaurant.getEstado()).isEqualTo(EstadoRestaurante.PENDIENTE);
        assertThat(restaurant.isAprobado()).isFalse();
        assertThat(restaurant.getStrikes()).isZero();
        assertThat(restaurant.getActivo()).isTrue();
        assertThat(usuarioRepo.findById(solicitud.usuarioId()).orElseThrow().getStrikes()).isZero();
        assertThat(usuarioRepo.findById(solicitud.usuarioId()).orElseThrow().getActivo()).isTrue();

        ResponseEntity<List> pendientes = send(HttpMethod.GET, "/api/admin/restaurantes/pendientes", admin(), null, List.class);
        assertThat(pendientes.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(pendientes.getBody()).anySatisfy(item -> {
            Map<?, ?> fila = (Map<?, ?>) item;
            assertThat(fila.get("nombre")).isEqualTo(solicitud.nombre());
            assertThat(fila.get("email")).isEqualTo(solicitud.email());
            assertThat(fila.get("estado")).isEqualTo("PENDIENTE");
        });
    }

    @Test
    void aprobarUnaSolicitudLaPublicaYHabilitaAlEncargado() {
        Solicitud solicitud = registrarRestaurante();

        ResponseEntity<Map> resp = send(HttpMethod.PUT,
                "/api/admin/restaurantes/" + solicitud.restaurantId() + "/aprobar", admin(), null, Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).containsEntry("estado", "APROBADO");
        Restaurant restaurant = restaurantRepo.findById(solicitud.restaurantId()).orElseThrow();
        assertThat(restaurant.getEstado()).isEqualTo(EstadoRestaurante.APROBADO);
        assertThat(restaurant.isAprobado()).isTrue();
        assertThat(usuarioRepo.findById(solicitud.usuarioId()).orElseThrow().getRol().name()).isEqualTo("ROLE_ENCARGADO");
        assertThat(nombresPendientes()).doesNotContain(solicitud.nombre());
    }

    @Test
    void rechazarUnaSolicitudLaSacaDePendientesSinPublicarla() {
        Solicitud solicitud = registrarRestaurante();

        ResponseEntity<Map> resp = send(HttpMethod.PUT,
                "/api/admin/restaurantes/" + solicitud.restaurantId() + "/rechazar", admin(), null, Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).containsEntry("estado", "RECHAZADO");
        Restaurant restaurant = restaurantRepo.findById(solicitud.restaurantId()).orElseThrow();
        assertThat(restaurant.getEstado()).isEqualTo(EstadoRestaurante.RECHAZADO);
        assertThat(restaurant.isAprobado()).isFalse();
        assertThat(nombresPendientes()).doesNotContain(solicitud.nombre());
    }

    @Test
    void noSePuedeRechazarUnRestauranteYaAprobado() {
        Solicitud solicitud = registrarRestaurante();
        send(HttpMethod.PUT, "/api/admin/restaurantes/" + solicitud.restaurantId() + "/aprobar", admin(), null, Map.class);

        ResponseEntity<Map> resp = send(HttpMethod.PUT,
                "/api/admin/restaurantes/" + solicitud.restaurantId() + "/rechazar", admin(), null, Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(restaurantRepo.findById(solicitud.restaurantId()).orElseThrow().getEstado())
                .isEqualTo(EstadoRestaurante.APROBADO);
    }

    @Test
    void elEndpointDeAprobacionExistenteTambienActualizaElEstado() {
        Solicitud solicitud = registrarRestaurante();

        ResponseEntity<UsuarioDetailDTO> resp = send(HttpMethod.PUT,
                "/api/admin/approved/" + solicitud.usuarioId(), admin(), null, UsuarioDetailDTO.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(restaurantRepo.findById(solicitud.restaurantId()).orElseThrow().getEstado())
                .isEqualTo(EstadoRestaurante.APROBADO);
        assertThat(nombresPendientes()).doesNotContain(solicitud.nombre());
    }

    @Test
    void cadaStrikeAUnUsuarioSumaYEnviaElAvisoPorEmail() {
        Long clienteId = registrarCliente();

        ResponseEntity<Map> resp = null;
        for (int i = 0; i < 3; i++) {
            resp = send(HttpMethod.POST, "/api/admin/usuarios/" + clienteId + "/strikes", admin(),
                    Map.of("motivo", "Lenguaje ofensivo en una reseña"), Map.class);
            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        assertThat(resp.getBody())
                .containsEntry("strikes", 3)
                .containsEntry("sugerirBaja", true)
                .containsEntry("emailEnviado", true);
        assertThat(usuarioRepo.findById(clienteId).orElseThrow().getStrikes()).isEqualTo(3);
        verify(mailSender, times(3)).send(any(SimpleMailMessage.class));
    }

    @Test
    void unStrikeAUnRestauranteAvisaAlEmailDelEncargado() {
        Solicitud solicitud = registrarRestaurante();

        ResponseEntity<Map> resp = send(HttpMethod.POST,
                "/api/admin/restaurantes/" + solicitud.restaurantId() + "/strikes", admin(),
                Map.of("motivo", "Reservas canceladas sin aviso"), Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody())
                .containsEntry("strikes", 1)
                .containsEntry("sugerirBaja", false)
                .containsEntry("email", solicitud.email());
        assertThat(restaurantRepo.findById(solicitud.restaurantId()).orElseThrow().getStrikes()).isEqualTo(1);
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void siFallaElEmailElStrikeIgualQuedaRegistrado() {
        Long clienteId = registrarCliente();
        doThrow(new MailSendException("SMTP caído")).when(mailSender).send(any(SimpleMailMessage.class));

        ResponseEntity<Map> resp = send(HttpMethod.POST, "/api/admin/usuarios/" + clienteId + "/strikes", admin(),
                Map.of("motivo", "Spam en reseñas"), Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).containsEntry("strikes", 1).containsEntry("emailEnviado", false);
        assertThat(usuarioRepo.findById(clienteId).orElseThrow().getStrikes()).isEqualTo(1);
    }

    @Test
    void unStrikeSinMotivoDevuelve400() {
        Long clienteId = registrarCliente();

        ResponseEntity<Map> resp = send(HttpMethod.POST, "/api/admin/usuarios/" + clienteId + "/strikes", admin(),
                Map.of("motivo", " "), Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(usuarioRepo.findById(clienteId).orElseThrow().getStrikes()).isZero();
    }

    @Test
    void noSePuedeDarUnStrikeAUnAdministrador() {
        Long adminId = usuarioRepo.findByEmail("admin@foodies.com").orElseThrow().getId();

        ResponseEntity<Map> resp = send(HttpMethod.POST, "/api/admin/usuarios/" + adminId + "/strikes", admin(),
                Map.of("motivo", "Prueba de strike"), Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void laUbicacionCargadaSeVeEnLaFichaPublica() {
        Solicitud solicitud = registrarRestaurante();
        send(HttpMethod.PUT, "/api/admin/restaurantes/" + solicitud.restaurantId() + "/aprobar", admin(), null, Map.class);

        ResponseEntity<Map> resp = send(HttpMethod.PUT,
                "/api/admin/restaurantes/" + solicitud.restaurantId() + "/ubicacion", admin(),
                Map.of("latitud", -38.0055, "longitud", -57.5426), Map.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<Map> ficha = rest.getForEntity("/api/restaurantes/" + solicitud.restaurantId(), Map.class);
        assertThat(ficha.getBody()).containsEntry("latitud", -38.0055).containsEntry("longitud", -57.5426);
    }

    @Test
    void unaUbicacionFueraDeRangoDevuelve400() {
        Solicitud solicitud = registrarRestaurante();

        ResponseEntity<Map> resp = send(HttpMethod.PUT,
                "/api/admin/restaurantes/" + solicitud.restaurantId() + "/ubicacion", admin(),
                Map.of("latitud", -120, "longitud", -57.5426), Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void unClienteNoPuedeModerar() {
        Long clienteId = registrarCliente();
        String token = login(usuarioRepo.findById(clienteId).orElseThrow().getEmail(), "secret1");

        ResponseEntity<Map> resp = send(HttpMethod.POST, "/api/admin/usuarios/" + clienteId + "/strikes", token,
                Map.of("motivo", "Auto strike"), Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(usuarioRepo.findById(clienteId).orElseThrow().getStrikes()).isZero();
    }

    @Test
    void elBackfillDerivaElEstadoDeLasFilasViejasDesdeAprobado() {
        Solicitud pendiente = registrarRestaurante();
        Solicitud aprobada = registrarRestaurante();
        send(HttpMethod.PUT, "/api/admin/approved/" + aprobada.usuarioId(), admin(), null, UsuarioDetailDTO.class);
        // Rows created before Phase 3 have no estado.
        jdbc.update("update restaurants set estado = null where id in (?, ?)", pendiente.restaurantId(), aprobada.restaurantId());

        backfill.run(null);

        assertThat(restaurantRepo.findById(pendiente.restaurantId()).orElseThrow().getEstado())
                .isEqualTo(EstadoRestaurante.PENDIENTE);
        assertThat(restaurantRepo.findById(aprobada.restaurantId()).orElseThrow().getEstado())
                .isEqualTo(EstadoRestaurante.APROBADO);
    }

    private List<Object> nombresPendientes() {
        ResponseEntity<List> pendientes = send(HttpMethod.GET, "/api/admin/restaurantes/pendientes", admin(), null, List.class);
        return ((List<?>) pendientes.getBody()).stream().map(item -> ((Map<?, ?>) item).get("nombre")).map(Object.class::cast).toList();
    }

    private Solicitud registrarRestaurante() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String email = "resto-" + suffix + "@test.com";
        String nombre = "Resto " + suffix;
        ResponseEntity<UsuarioDetailDTO> reg = rest.postForEntity("/api/auth/register/restaurante", Map.of(
                "email", email,
                "password", "secret1",
                "nombreRestaurante", nombre,
                "direccion", "Calle " + suffix,
                "telefono", "2230000000",
                "especialidadDeComida", "PASTAS",
                "cupoMaximo", 10), UsuarioDetailDTO.class);
        assertThat(reg.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Long restaurantId = restaurantRepo.findAll().stream()
                .filter(r -> r.getNombre().equals(nombre))
                .map(Restaurant::getId)
                .findFirst()
                .orElseThrow();
        return new Solicitud(reg.getBody().id(), restaurantId, nombre, email);
    }

    private Long registrarCliente() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        ResponseEntity<UsuarioDetailDTO> reg = rest.postForEntity("/api/auth/register/cliente", Map.of(
                "nombre", "Cliente",
                "apellido", suffix,
                "email", "cliente-" + suffix + "@test.com",
                "password", "secret1",
                "telefono", "2231111111"), UsuarioDetailDTO.class);
        assertThat(reg.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return reg.getBody().id();
    }

    private String admin() {
        return login("admin@foodies.com", "admin123");
    }

    private String login(String email, String password) {
        ResponseEntity<Map> resp = rest.postForEntity("/api/auth/login", Map.of("email", email, "password", password), Map.class);
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
