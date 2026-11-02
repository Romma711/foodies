package com.example.Foodies.Moderacion;

import com.example.Foodies.RegistroRestauranteMultipart;
import com.example.Foodies.Restaurant.Restaurant;
import com.example.Foodies.Restaurant.RestaurantRepository;
import com.example.Foodies.Usuario.dtos.UsuarioDetailDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

/**
 * End-to-end checks of the confirmation emails: the manager hears when the admin approves the
 * restaurant, and the client hears when the restaurant accepts the reservation.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class NotificacionesEmailTest {

    @Autowired
    private TestRestTemplate rest;
    @Autowired
    private RestaurantRepository restaurantRepo;

    @MockitoBean
    private JavaMailSender mailSender;

    private record Cuenta(Long id, String email, String nombre) {}

    @BeforeEach
    void limpiarCorreo() {
        reset(mailSender);
    }

    @Test
    void aprobarUnRestauranteLeAvisaAlEncargado() {
        Cuenta encargado = registrarRestaurante();
        Long id = idRestaurante(encargado.nombre());

        ResponseEntity<Map> resp = send(HttpMethod.PUT, "/api/admin/restaurantes/" + id + "/aprobar", admin(), null, Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        SimpleMailMessage mail = unicoMailPara(encargado.email());
        assertThat(mail.getSubject()).isEqualTo("Foodies: tu restaurante " + encargado.nombre() + " fue aprobado");
        assertThat(mail.getText())
                .contains(encargado.nombre())
                .contains("ya aparece en Foodies")
                .contains("panel");
    }

    @Test
    void aprobarDeNuevoUnRestauranteYaAprobadoNoRepiteElAviso() {
        Cuenta encargado = registrarRestaurante();
        Long id = idRestaurante(encargado.nombre());
        send(HttpMethod.PUT, "/api/admin/restaurantes/" + id + "/aprobar", admin(), null, Map.class);

        send(HttpMethod.PUT, "/api/admin/restaurantes/" + id + "/aprobar", admin(), null, Map.class);

        assertThat(mailsPara(encargado.email())).hasSize(1);
    }

    @Test
    void confirmarUnaReservaLeAvisaAlCliente() {
        Escenario escenario = reservaPendiente();

        ResponseEntity<Map> resp = cambiarEstado(escenario, "ACEPTADA");

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).containsEntry("estado", "ACEPTADA");
        SimpleMailMessage mail = unicoMailPara(escenario.cliente().email());
        assertThat(mail.getSubject()).isEqualTo("Foodies: tu reserva en " + escenario.restaurante() + " está confirmada");
        assertThat(mail.getText())
                .contains("Hola, Cliente")
                .contains(escenario.restaurante())
                .contains("Fecha: " + LocalDate.parse(escenario.fecha()).format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                .contains("Hora: 20:30")
                .contains("Personas: 3");
    }

    @Test
    void unaReservaAceptadaAutomaticamenteTambienLeAvisaAlCliente() {
        Cuenta encargado = registrarRestaurante();
        Long restaurantId = idRestaurante(encargado.nombre());
        send(HttpMethod.PUT, "/api/admin/restaurantes/" + restaurantId + "/aprobar", admin(), null, Map.class);
        send(HttpMethod.PATCH, "/api/restaurantes/me", login(encargado.email()),
                Map.of("porcentajeAutoAceptacion", 100), Map.class);
        Cuenta cliente = registrarCliente();
        clearInvocations(mailSender);

        ResponseEntity<Map> reserva = send(HttpMethod.POST, "/api/reservas", login(cliente.email()), Map.of(
                "cantidad", 3,
                "fechaReserva", LocalDate.now().plusDays(2).toString(),
                "horarioLlegada", "20:30",
                "idUsuario", cliente.id(),
                "idRestaurant", restaurantId), Map.class);

        assertThat(reserva.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(reserva.getBody()).containsEntry("estadoReserva", "ACEPTADA");
        assertThat(unicoMailPara(cliente.email()).getSubject())
                .isEqualTo("Foodies: tu reserva en " + encargado.nombre() + " está confirmada");
    }

    @Test
    void cancelarUnaReservaNoEnviaLaConfirmacion() {
        Escenario escenario = reservaPendiente();

        cambiarEstado(escenario, "CANCELADA");

        assertThat(mailsPara(escenario.cliente().email())).isEmpty();
    }

    @Test
    void siElCorreoFallaLaReservaIgualQuedaConfirmada() {
        Escenario escenario = reservaPendiente();
        doThrow(new MailSendException("smtp caído")).when(mailSender).send(any(SimpleMailMessage.class));

        ResponseEntity<Map> resp = cambiarEstado(escenario, "ACEPTADA");

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).containsEntry("estado", "ACEPTADA");
        ResponseEntity<Map> reserva = send(HttpMethod.GET, "/api/reservas/" + escenario.reservaId(), escenario.tokenCliente(), null, Map.class);
        assertThat(reserva.getBody()).containsEntry("estadoReserva", "ACEPTADA");
    }

    private record Escenario(Cuenta cliente, String tokenCliente, String restaurante, String tokenEncargado,
                             Long reservaId, String fecha) {}

    /** An approved restaurant with a pending reservation of a client; the mail mock starts clean. */
    private Escenario reservaPendiente() {
        Cuenta encargado = registrarRestaurante();
        Long restaurantId = idRestaurante(encargado.nombre());
        send(HttpMethod.PUT, "/api/admin/restaurantes/" + restaurantId + "/aprobar", admin(), null, Map.class);
        Cuenta cliente = registrarCliente();
        String tokenCliente = login(cliente.email());
        String fecha = LocalDate.now().plusDays(2).toString();
        ResponseEntity<Map> reserva = send(HttpMethod.POST, "/api/reservas", tokenCliente, Map.of(
                "cantidad", 3,
                "fechaReserva", fecha,
                "horarioLlegada", "20:30",
                "idUsuario", cliente.id(),
                "idRestaurant", restaurantId), Map.class);
        assertThat(reserva.getStatusCode()).isEqualTo(HttpStatus.OK);
        Long reservaId = ((Number) reserva.getBody().get("id")).longValue();
        clearInvocations(mailSender);
        return new Escenario(cliente, tokenCliente, encargado.nombre(), login(encargado.email()), reservaId, fecha);
    }

    private ResponseEntity<Map> cambiarEstado(Escenario escenario, String estado) {
        return send(HttpMethod.PATCH, "/api/reservas/" + escenario.reservaId() + "/estado", escenario.tokenEncargado(),
                Map.of("estado", estado), Map.class);
    }

    private SimpleMailMessage unicoMailPara(String email) {
        List<SimpleMailMessage> mails = mailsPara(email);
        assertThat(mails).hasSize(1);
        return mails.get(0);
    }

    private List<SimpleMailMessage> mailsPara(String email) {
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        try {
            verify(mailSender, atLeast(1)).send(captor.capture());
        } catch (AssertionError sinEnvios) {
            return List.of();
        }
        return captor.getAllValues().stream()
                .filter(mail -> mail.getTo() != null && List.of(mail.getTo()).contains(email))
                .toList();
    }

    private Long idRestaurante(String nombre) {
        return restaurantRepo.findAll().stream()
                .filter(r -> r.getNombre().equals(nombre))
                .map(Restaurant::getId)
                .findFirst()
                .orElseThrow();
    }

    private Cuenta registrarRestaurante() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String email = "resto-" + suffix + "@test.com";
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
        return new Cuenta(reg.getBody().id(), email, nombre);
    }

    private Cuenta registrarCliente() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String email = "cliente-" + suffix + "@test.com";
        ResponseEntity<UsuarioDetailDTO> reg = rest.postForEntity("/api/auth/register/cliente", Map.of(
                "nombre", "Cliente",
                "apellido", suffix,
                "email", email,
                "password", "secret1",
                "telefono", "2231111111"), UsuarioDetailDTO.class);
        assertThat(reg.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return new Cuenta(reg.getBody().id(), email, "Cliente");
    }

    private String admin() {
        return token("admin@foodies.com", "admin123");
    }

    private String login(String email) {
        return token(email, "secret1");
    }

    private String token(String email, String password) {
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
