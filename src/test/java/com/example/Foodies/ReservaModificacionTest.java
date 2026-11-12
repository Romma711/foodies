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
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** S2: the owner modifies or cancels a reservation; the server validates the new data. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ReservaModificacionTest {

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
    private static final LocalDate PASADO_MANIANA = LocalDate.now().plusDays(2);

    @Test
    void elDuenioCambiaFechaHoraYCantidadYLaReservaVuelveAPendiente() {
        Encargado encargado = api.nuevoEncargado();
        assertThat(api.status(HttpMethod.PATCH, "/api/restaurantes/me", encargado.token(),
                Map.of("porcentajeAutoAceptacion", 100))).isEqualTo(HttpStatus.OK);
        Cliente cliente = api.nuevoCliente();
        ResponseEntity<Map> creada = api.reservar(cliente, encargado.restaurantId(), MANIANA, 2);
        assertThat(creada.getBody()).containsEntry("estadoReserva", "ACEPTADA");
        Long id = ((Number) creada.getBody().get("id")).longValue();

        ResponseEntity<Map> modificada = api.send(HttpMethod.PUT, "/api/reservas/" + id, cliente.token(), Map.of(
                "fechaReserva", PASADO_MANIANA.toString(),
                "horarioLlegada", "21:15",
                "cantidad", 4), Map.class);

        assertThat(modificada.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(modificada.getBody())
                .containsEntry("cantidad", 4)
                .containsEntry("estadoReserva", "PENDIENTE")
                .containsEntry("horariollegada", "21:15:00");
        Reserva guardada = reservaRepo.findById(id).orElseThrow();
        assertThat(guardada.getCantidad()).isEqualTo(4);
        assertThat(guardada.getHorariollegada()).isEqualTo(LocalTime.of(21, 15));
        assertThat(new SimpleDateFormat("yyyy-MM-dd").format(guardada.getFechaReserva())).isEqualTo(PASADO_MANIANA.toString());
        assertThat(guardada.getEstadoReserva()).isEqualTo(EstadoReserva.PENDIENTE);
    }

    @Test
    void elCupoNoCuentaLaMismaReservaQueSeModifica() {
        Encargado encargado = api.nuevoEncargado();
        Cliente cliente = api.nuevoCliente();
        Long id = api.crearReserva(cliente, encargado.restaurantId(), MANIANA, 8);

        // Capacity is 10: 10 fits only if the reservation's own 8 people are not counted again.
        assertThat(api.status(HttpMethod.PUT, "/api/reservas/" + id, cliente.token(), Map.of("cantidad", 10)))
                .isEqualTo(HttpStatus.OK);
        assertThat(reservaRepo.findById(id).orElseThrow().getCantidad()).isEqualTo(10);
    }

    @Test
    void rechazaCantidadCeroFechaPasadaYFaltaDeCupoCon400() {
        Encargado encargado = api.nuevoEncargado();
        Cliente cliente = api.nuevoCliente();
        Cliente otro = api.nuevoCliente();
        Long id = api.crearReserva(cliente, encargado.restaurantId(), MANIANA, 2);
        api.crearReserva(otro, encargado.restaurantId(), PASADO_MANIANA, 6);
        String path = "/api/reservas/" + id;

        ResponseEntity<Map> cero = api.send(HttpMethod.PUT, path, cliente.token(), Map.of("cantidad", 0), Map.class);
        assertThat(cero.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(cero.getBody()).containsEntry("mensaje", "La cantidad de personas debe ser mayor a 0");

        ResponseEntity<Map> pasada = api.send(HttpMethod.PUT, path, cliente.token(),
                Map.of("fechaReserva", LocalDate.now().minusDays(1).toString()), Map.class);
        assertThat(pasada.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(pasada.getBody()).containsEntry("mensaje", "Fecha no válida: No puede ser anterior al día de hoy");

        ResponseEntity<Map> sinLugar = api.send(HttpMethod.PUT, path, cliente.token(),
                Map.of("fechaReserva", PASADO_MANIANA.toString(), "cantidad", 5), Map.class);
        assertThat(sinLugar.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat((String) sinLugar.getBody().get("mensaje")).startsWith("No hay lugar para 5 personas");

        ResponseEntity<Map> horaInvalida = api.send(HttpMethod.PUT, path, cliente.token(),
                Map.of("horarioLlegada", "25:99"), Map.class);
        assertThat(horaInvalida.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        Reserva intacta = reservaRepo.findById(id).orElseThrow();
        assertThat(intacta.getCantidad()).isEqualTo(2);
        assertThat(new SimpleDateFormat("yyyy-MM-dd").format(intacta.getFechaReserva())).isEqualTo(MANIANA.toString());
    }

    @Test
    void elClienteSoloPuedeCancelarYNuncaAceptar() {
        Encargado encargado = api.nuevoEncargado();
        Cliente cliente = api.nuevoCliente();
        Long id = api.crearReserva(cliente, encargado.restaurantId(), MANIANA, 2);
        String path = "/api/reservas/" + id;

        ResponseEntity<Map> aceptar = api.send(HttpMethod.PUT, path, cliente.token(),
                Map.of("estadoReserva", "ACEPTADA"), Map.class);
        assertThat(aceptar.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(reservaRepo.findById(id).orElseThrow().getEstadoReserva()).isEqualTo(EstadoReserva.PENDIENTE);

        ResponseEntity<Map> cancelar = api.send(HttpMethod.PUT, path, cliente.token(),
                Map.of("estadoReserva", "CANCELADA"), Map.class);
        assertThat(cancelar.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(cancelar.getBody()).containsEntry("estadoReserva", "CANCELADA");
    }

    @Test
    void unaReservaCanceladaNoSePuedeModificar() {
        Encargado encargado = api.nuevoEncargado();
        Cliente cliente = api.nuevoCliente();
        Long id = api.crearReserva(cliente, encargado.restaurantId(), MANIANA, 2);
        String path = "/api/reservas/" + id;
        api.send(HttpMethod.PUT, path, cliente.token(), Map.of("estadoReserva", "CANCELADA"), Map.class);

        ResponseEntity<Map> modificar = api.send(HttpMethod.PUT, path, cliente.token(), Map.of("cantidad", 3), Map.class);

        assertThat(modificar.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(modificar.getBody()).containsEntry("mensaje", "La reserva está cancelada y no se puede modificar");
        assertThat(reservaRepo.findById(id).orElseThrow().getEstadoReserva()).isEqualTo(EstadoReserva.CANCELADA);
    }
}
