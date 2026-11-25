package com.example.Foodies;

import com.example.Foodies.AutorizacionTestSupport.Cliente;
import com.example.Foodies.AutorizacionTestSupport.Encargado;
import com.example.Foodies.Enums.EstadoReserva;
import com.example.Foodies.Reserva.ReservaRepository;
import com.example.Foodies.Reserva.VencimientoReservas;
import com.example.Foodies.Restaurant.RestaurantRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** BL-07 / D4: a PENDIENTE nobody accepted before its arrival time is closed as VENCIDA by a scheduled job. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(RelojDePrueba.Config.class)
class VencimientoReservasTest {

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
    @Autowired
    private RelojDePrueba reloj;
    @Autowired
    private VencimientoReservas vencimiento;

    private AutorizacionTestSupport api;
    private static final LocalDate MANIANA = LocalDate.now().plusDays(1);

    @BeforeEach
    void setUp() {
        reloj.reiniciar();
        api = new AutorizacionTestSupport(rest, restaurantRepo);
    }

    @AfterEach
    void tearDown() {
        reloj.reiniciar();
    }

    private EstadoReserva estado(Long id) {
        return reservaRepo.findById(id).orElseThrow().getEstadoReserva();
    }

    @Test
    void lasPendientesCuyaLlegadaPasoQuedanVencidasYLasDemasNoCambian() {
        Encargado encargado = api.nuevoEncargado();
        Long pendiente = api.crearReserva(api.nuevoCliente(), encargado.restaurantId(), MANIANA, 2);
        Long aceptada = api.crearReserva(api.nuevoCliente(), encargado.restaurantId(), MANIANA, 2);
        Long futura = api.crearReserva(api.nuevoCliente(), encargado.restaurantId(), MANIANA.plusDays(1), 2);
        api.send(HttpMethod.PATCH, "/api/reservas/" + aceptada + "/estado", encargado.token(),
                Map.of("estado", "ACEPTADA"), Map.class);

        reloj.fijar(MANIANA.atTime(19, 59));
        vencimiento.vencerPendientes();
        assertThat(estado(pendiente)).isEqualTo(EstadoReserva.PENDIENTE);

        reloj.fijar(MANIANA.atTime(20, 1));
        assertThat(vencimiento.vencerPendientes()).isPositive();

        assertThat(estado(pendiente)).isEqualTo(EstadoReserva.VENCIDA);
        assertThat(estado(aceptada)).isEqualTo(EstadoReserva.ACEPTADA);
        assertThat(estado(futura)).isEqualTo(EstadoReserva.PENDIENTE);
    }

    @Test
    void unaReservaVencidaYaNoSePuedeCancelarNiAceptar() {
        Encargado encargado = api.nuevoEncargado();
        Cliente cliente = api.nuevoCliente();
        Long id = api.crearReserva(cliente, encargado.restaurantId(), MANIANA, 2);
        reloj.fijar(MANIANA.atTime(21, 0));
        vencimiento.vencerPendientes();

        ResponseEntity<Map> cancelar = api.send(HttpMethod.PUT, "/api/reservas/" + id, cliente.token(),
                Map.of("estadoReserva", "CANCELADA"), Map.class);
        ResponseEntity<Map> aceptar = api.send(HttpMethod.PATCH, "/api/reservas/" + id + "/estado", encargado.token(),
                Map.of("estado", "ACEPTADA"), Map.class);

        assertThat(cancelar.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(cancelar.getBody()).containsEntry("mensaje", "La reserva ya está cerrada y no se puede modificar");
        assertThat(aceptar.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(aceptar.getBody()).containsEntry("mensaje", "La reserva ya está vencida y no se puede cambiar");
        assertThat(estado(id)).isEqualTo(EstadoReserva.VENCIDA);
    }
}
