package com.example.Foodies;

import com.example.Foodies.AutorizacionTestSupport.Cliente;
import com.example.Foodies.AutorizacionTestSupport.Encargado;
import com.example.Foodies.Restaurant.Restaurant;
import com.example.Foodies.Restaurant.RestaurantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

/** BL-18 / D10: who hears about each reservation change. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AvisosReservaTest {

    @TempDir
    static Path storageDir;

    @DynamicPropertySource
    static void storage(DynamicPropertyRegistry registry) {
        registry.add("storage.provider", () -> "local");
        registry.add("storage.local.dir", () -> storageDir.toString());
    }

    @Autowired
    private TestRestTemplate rest;
    @Autowired
    private RestaurantRepository restaurantRepo;
    @MockitoBean
    private JavaMailSender mailSender;

    private AutorizacionTestSupport api;
    private static final LocalDate FECHA = LocalDate.now().plusDays(2);

    @BeforeEach
    void setUp() {
        reset(mailSender);
        api = new AutorizacionTestSupport(rest, restaurantRepo);
    }

    private List<SimpleMailMessage> mailsPara(String email) {
        if (mockingDetails(mailSender).getInvocations().isEmpty()) {
            return List.of();
        }
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, atLeast(0)).send(captor.capture());
        return captor.getAllValues().stream()
                .filter(m -> m.getTo() != null && Arrays.asList(m.getTo()).contains(email))
                .toList();
    }

    private String emailEncargado(Encargado encargado) {
        Restaurant restaurant = restaurantRepo.findById(encargado.restaurantId()).orElseThrow();
        return restaurant.getUsuario().getEmail();
    }

    private String nombre(Encargado encargado) {
        return restaurantRepo.findById(encargado.restaurantId()).orElseThrow().getNombre();
    }

    private void cambiarEstado(Encargado encargado, Long reservaId, String estado) {
        assertThat(api.send(HttpMethod.PATCH, "/api/reservas/" + reservaId + "/estado", encargado.token(),
                Map.of("estado", estado), Map.class).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void unaReservaNuevaLeAvisaAlRestaurante() {
        Encargado encargado = api.nuevoEncargado();
        Cliente cliente = api.nuevoCliente();
        clearInvocations(mailSender);

        api.crearReserva(cliente, encargado.restaurantId(), FECHA, 3);

        List<SimpleMailMessage> mails = mailsPara(emailEncargado(encargado));
        assertThat(mails).hasSize(1);
        assertThat(mails.get(0).getSubject()).isEqualTo("Foodies: nueva reserva en " + nombre(encargado));
        assertThat(mails.get(0).getText()).contains("Personas: 3").contains("pendiente de confirmación");
    }

    @Test
    void rechazarLeAvisaAlCliente() {
        Encargado encargado = api.nuevoEncargado();
        Cliente cliente = api.nuevoCliente();
        Long id = api.crearReserva(cliente, encargado.restaurantId(), FECHA, 2);

        cambiarEstado(encargado, id, "RECHAZADA");

        List<SimpleMailMessage> mails = mailsPara(cliente.email());
        assertThat(mails).hasSize(1);
        assertThat(mails.get(0).getSubject()).isEqualTo("Foodies: tu reserva en " + nombre(encargado) + " no fue confirmada");
    }

    @Test
    void cancelarDesdeElRestauranteLeAvisaAlCliente() {
        Encargado encargado = api.nuevoEncargado();
        Cliente cliente = api.nuevoCliente();
        Long id = api.crearReserva(cliente, encargado.restaurantId(), FECHA, 2);
        cambiarEstado(encargado, id, "ACEPTADA");
        clearInvocations(mailSender);

        cambiarEstado(encargado, id, "CANCELADA");

        List<SimpleMailMessage> mails = mailsPara(cliente.email());
        assertThat(mails).hasSize(1);
        assertThat(mails.get(0).getSubject()).isEqualTo("Foodies: " + nombre(encargado) + " canceló tu reserva");
    }

    @Test
    void cancelarDesdeElClienteLeAvisaAlRestaurante() {
        Encargado encargado = api.nuevoEncargado();
        Cliente cliente = api.nuevoCliente();
        Long id = api.crearReserva(cliente, encargado.restaurantId(), FECHA, 2);
        clearInvocations(mailSender);

        api.send(HttpMethod.PUT, "/api/reservas/" + id, cliente.token(), Map.of("estadoReserva", "CANCELADA"), Map.class);

        List<SimpleMailMessage> mails = mailsPara(emailEncargado(encargado));
        assertThat(mails).hasSize(1);
        assertThat(mails.get(0).getSubject()).isEqualTo("Foodies: se canceló una reserva en " + nombre(encargado));
        assertThat(mails.get(0).getText()).contains("canceló su reserva");
        assertThat(mailsPara(cliente.email())).isEmpty();
    }

    @Test
    void lasBajasLeAvisanALaOtraParte() {
        Encargado encargado = api.nuevoEncargado();
        Encargado otro = api.nuevoEncargado();
        Cliente cliente = api.nuevoCliente();
        Cliente otroCliente = api.nuevoCliente();
        api.crearReserva(cliente, encargado.restaurantId(), FECHA, 2);
        api.crearReserva(otroCliente, otro.restaurantId(), FECHA, 2);
        String admin = api.admin();
        clearInvocations(mailSender);

        api.send(HttpMethod.PATCH, "/api/admin/usuarios/" + cliente.id() + "/activo", admin, Map.of("activo", false), Map.class);
        api.send(HttpMethod.PATCH, "/api/admin/restaurantes/" + otro.restaurantId() + "/activo", admin,
                Map.of("activo", false), Map.class);

        List<SimpleMailMessage> alRestaurante = mailsPara(emailEncargado(encargado));
        assertThat(alRestaurante).hasSize(1);
        assertThat(alRestaurante.get(0).getText()).contains("fue dada de baja");
        List<SimpleMailMessage> alCliente = mailsPara(otroCliente.email());
        assertThat(alCliente).hasSize(1);
        assertThat(alCliente.get(0).getSubject()).isEqualTo("Foodies: tu reserva en " + nombre(otro) + " fue cancelada");
    }
}
