package com.example.Foodies;

import com.example.Foodies.AutorizacionTestSupport.Cliente;
import com.example.Foodies.AutorizacionTestSupport.Encargado;
import com.example.Foodies.Enums.EstadoReserva;
import com.example.Foodies.Reserva.Reserva;
import com.example.Foodies.Reserva.ReservaRepository;
import com.example.Foodies.Restaurant.RestaurantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** S1: a reservation belongs to the authenticated client that created it. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ReservaAutorizacionTest {

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

    @Autowired
    private ReservaRepository reservaRepo;

    private AutorizacionTestSupport api;

    @BeforeEach
    void setUp() {
        api = new AutorizacionTestSupport(rest, restaurantRepo);
    }

    private static final LocalDate MANIANA = LocalDate.now().plusDays(1);

    @Test
    void elDuenioVeSuReservaYOtroClienteNo() {
        Encargado encargado = api.nuevoEncargado();
        Cliente duenio = api.nuevoCliente();
        Cliente otro = api.nuevoCliente();
        Long reservaId = api.crearReserva(duenio, encargado.restaurantId(), MANIANA, 2);

        assertThat(api.status(HttpMethod.GET, "/api/reservas/" + reservaId, duenio.token(), null)).isEqualTo(HttpStatus.OK);
        assertThat(api.status(HttpMethod.GET, "/api/reservas/" + reservaId, otro.token(), null)).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(api.status(HttpMethod.GET, "/api/reservas/usuario/" + duenio.id(), duenio.token(), null)).isEqualTo(HttpStatus.OK);
        assertThat(api.status(HttpMethod.GET, "/api/reservas/usuario/" + duenio.id(), otro.token(), null)).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void elEncargadoDelRestauranteYElAdminVenLaReservaPeroOtroEncargadoNo() {
        Encargado encargado = api.nuevoEncargado();
        Encargado otroEncargado = api.nuevoEncargado();
        Cliente cliente = api.nuevoCliente();
        Long reservaId = api.crearReserva(cliente, encargado.restaurantId(), MANIANA, 2);

        assertThat(api.status(HttpMethod.GET, "/api/reservas/" + reservaId, encargado.token(), null)).isEqualTo(HttpStatus.OK);
        assertThat(api.status(HttpMethod.GET, "/api/reservas/" + reservaId, otroEncargado.token(), null)).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(api.status(HttpMethod.GET, "/api/reservas/" + reservaId, api.admin(), null)).isEqualTo(HttpStatus.OK);
        assertThat(api.status(HttpMethod.GET, "/api/reservas/usuario/" + cliente.id(), api.admin(), null)).isEqualTo(HttpStatus.OK);
    }

    @Test
    void soloElAdminListaTodasLasReservas() {
        Cliente cliente = api.nuevoCliente();

        assertThat(api.status(HttpMethod.GET, "/api/reservas", cliente.token(), null)).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(api.status(HttpMethod.GET, "/api/reservas", api.admin(), null)).isEqualTo(HttpStatus.OK);
    }

    @Test
    void lasReservasDeUnRestauranteLasVenSuEncargadoYElAdmin() {
        Encargado encargado = api.nuevoEncargado();
        Encargado otroEncargado = api.nuevoEncargado();
        Cliente cliente = api.nuevoCliente();
        api.crearReserva(cliente, encargado.restaurantId(), MANIANA, 2);
        String path = "/api/reservas/restaurante/" + encargado.restaurantId();

        assertThat(api.status(HttpMethod.GET, path, encargado.token(), null)).isEqualTo(HttpStatus.OK);
        assertThat(api.status(HttpMethod.GET, path, api.admin(), null)).isEqualTo(HttpStatus.OK);
        assertThat(api.status(HttpMethod.GET, path, otroEncargado.token(), null)).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(api.status(HttpMethod.GET, path, cliente.token(), null)).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void laReservaQuedaANombreDelUsuarioDelTokenYNoDeOtro() {
        Encargado encargado = api.nuevoEncargado();
        Cliente cliente = api.nuevoCliente();
        Cliente victima = api.nuevoCliente();

        ResponseEntity<Map> ajena = api.send(HttpMethod.POST, "/api/reservas", cliente.token(), Map.of(
                "cantidad", 2,
                "fechaReserva", MANIANA.toString(),
                "horarioLlegada", "20:00",
                "idUsuario", victima.id(),
                "idRestaurant", encargado.restaurantId()), Map.class);
        assertThat(ajena.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(reservaRepo.findAllByUsuario_Id(victima.id())).isEmpty();

        ResponseEntity<Map> sinUsuario = api.send(HttpMethod.POST, "/api/reservas", cliente.token(), Map.of(
                "cantidad", 2,
                "fechaReserva", MANIANA.toString(),
                "horarioLlegada", "20:00",
                "idRestaurant", encargado.restaurantId(),
                "idEspacio", api.primerEspacio(encargado.restaurantId())), Map.class);
        assertThat(sinUsuario.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(reservaRepo.findAllByUsuario_Id(cliente.id())).hasSize(1);
    }

    @Test
    void otroClienteNoPuedeModificarNiBorrarUnaReservaAjena() {
        Encargado encargado = api.nuevoEncargado();
        Cliente duenio = api.nuevoCliente();
        Cliente otro = api.nuevoCliente();
        Long reservaId = api.crearReserva(duenio, encargado.restaurantId(), MANIANA, 2);
        String path = "/api/reservas/" + reservaId;

        assertThat(api.status(HttpMethod.PUT, path, otro.token(), Map.of("cantidad", 3))).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(api.status(HttpMethod.PUT, path, otro.token(), Map.of("estadoReserva", "CANCELADA")))
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(api.status(HttpMethod.DELETE, path, otro.token(), null)).isEqualTo(HttpStatus.FORBIDDEN);

        Reserva intacta = reservaRepo.findById(reservaId).orElseThrow();
        assertThat(intacta.getCantidad()).isEqualTo(2);
        assertThat(intacta.getEstadoReserva()).isEqualTo(EstadoReserva.PENDIENTE);
    }

    @Test
    void elDuenioYElAdminPuedenBorrarLaReserva() {
        Encargado encargado = api.nuevoEncargado();
        Cliente duenio = api.nuevoCliente();
        Long propia = api.crearReserva(duenio, encargado.restaurantId(), MANIANA, 2);
        Long otra = api.crearReserva(duenio, encargado.restaurantId(), MANIANA, 2);

        assertThat(api.status(HttpMethod.DELETE, "/api/reservas/" + propia, duenio.token(), null)).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(api.status(HttpMethod.DELETE, "/api/reservas/" + otra, api.admin(), null)).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(reservaRepo.findAllById(List.of(propia, otra))).isEmpty();
    }
}
