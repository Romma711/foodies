package com.example.Foodies;

import com.example.Foodies.AutorizacionTestSupport.Cliente;
import com.example.Foodies.AutorizacionTestSupport.Encargado;
import com.example.Foodies.Enums.EstadoReserva;
import com.example.Foodies.Reserva.ReservaRepository;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

/** Reservation state machine (D4), not-past rule (BL-03) and capacity under concurrency (D17). */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(RelojDePrueba.Config.class)
class CicloDeVidaReservaTest {

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

    private ResponseEntity<Map> cambiarEstado(Encargado encargado, Long reservaId, String estado) {
        return api.send(HttpMethod.PATCH, "/api/reservas/" + reservaId + "/estado", encargado.token(),
                Map.of("estado", estado), Map.class);
    }

    private EstadoReserva estado(Long reservaId) {
        return reservaRepo.findById(reservaId).orElseThrow().getEstadoReserva();
    }

    @Test
    void bl03NoSePuedeReservarHoyEnUnHorarioQueYaPaso() {
        Encargado encargado = api.nuevoEncargado();
        Cliente cliente = api.nuevoCliente();
        // "Now" is tomorrow 21:00; the helper books tomorrow at 20:00.
        reloj.fijar(MANIANA.atTime(21, 0));

        ResponseEntity<Map> resp = api.reservar(cliente, encargado.restaurantId(), MANIANA, 2);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody()).containsEntry("mensaje",
                "La fecha y hora de la reserva no pueden ser anteriores a este momento");
        assertThat(reservaRepo.findAllByRestaurant_Id(encargado.restaurantId())).isEmpty();
    }

    @Test
    void bl04Bl05ElRestauranteNoPuedeAceptarUnaReservaCancelada() {
        Encargado encargado = api.nuevoEncargado();
        Cliente cliente = api.nuevoCliente();
        Cliente otro = api.nuevoCliente();
        Long cancelada = api.crearReserva(cliente, encargado.restaurantId(), MANIANA, 10);
        assertThat(api.send(HttpMethod.PUT, "/api/reservas/" + cancelada, cliente.token(),
                Map.of("estadoReserva", "CANCELADA"), Map.class).getStatusCode()).isEqualTo(HttpStatus.OK);
        api.crearReserva(otro, encargado.restaurantId(), MANIANA, 10);

        ResponseEntity<Map> resp = cambiarEstado(encargado, cancelada, "ACEPTADA");

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody()).containsEntry("mensaje", "La reserva ya está cancelada y no se puede cambiar");
        assertThat(estado(cancelada)).isEqualTo(EstadoReserva.CANCELADA);
    }

    @Test
    void bl06ElRestauranteNoPuedeAceptarUnaReservaCuyaHoraYaPaso() {
        Encargado encargado = api.nuevoEncargado();
        Cliente cliente = api.nuevoCliente();
        Long id = api.crearReserva(cliente, encargado.restaurantId(), MANIANA, 2);
        reloj.fijar(MANIANA.atTime(20, 30));

        ResponseEntity<Map> resp = cambiarEstado(encargado, id, "ACEPTADA");

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody()).containsEntry("mensaje",
                "La hora de la reserva ya pasó: solo podés marcar si el cliente asistió o no");
        assertThat(estado(id)).isEqualTo(EstadoReserva.PENDIENTE);
    }

    @Test
    void bl19ElRestauranteRechazaUnaPendienteYQuedaRechazadaComoEstadoFinal() {
        Encargado encargado = api.nuevoEncargado();
        Cliente cliente = api.nuevoCliente();
        Long id = api.crearReserva(cliente, encargado.restaurantId(), MANIANA, 2);

        ResponseEntity<Map> rechazada = cambiarEstado(encargado, id, "RECHAZADA");
        assertThat(rechazada.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(rechazada.getBody()).containsEntry("estado", "RECHAZADA");
        assertThat(estado(id)).isEqualTo(EstadoReserva.RECHAZADA);

        ResponseEntity<Map> aceptar = cambiarEstado(encargado, id, "ACEPTADA");
        assertThat(aceptar.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(aceptar.getBody()).containsEntry("mensaje", "La reserva ya está rechazada y no se puede cambiar");
        assertThat(estado(id)).isEqualTo(EstadoReserva.RECHAZADA);
    }

    @Test
    void unaReservaAceptadaNoSeRechazaSeCancela() {
        Encargado encargado = api.nuevoEncargado();
        Cliente cliente = api.nuevoCliente();
        Long id = api.crearReserva(cliente, encargado.restaurantId(), MANIANA, 2);
        assertThat(cambiarEstado(encargado, id, "ACEPTADA").getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<Map> rechazar = cambiarEstado(encargado, id, "RECHAZADA");
        assertThat(rechazar.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(rechazar.getBody()).containsEntry("mensaje",
                "Solo podés rechazar una reserva pendiente; una aceptada se cancela");

        ResponseEntity<Map> cancelar = cambiarEstado(encargado, id, "CANCELADA");
        assertThat(cancelar.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(estado(id)).isEqualTo(EstadoReserva.CANCELADA);
    }

    @Test
    void d4LaAsistenciaSeMarcaDesdeLaLlegadaHasta48HorasDespues() {
        Encargado encargado = api.nuevoEncargado();
        Cliente cliente = api.nuevoCliente();
        Cliente otro = api.nuevoCliente();
        Long id = api.crearReserva(cliente, encargado.restaurantId(), MANIANA, 2);
        Long tardia = api.crearReserva(otro, encargado.restaurantId(), MANIANA, 2);
        cambiarEstado(encargado, id, "ACEPTADA");
        cambiarEstado(encargado, tardia, "ACEPTADA");

        ResponseEntity<Map> antes = cambiarEstado(encargado, id, "ASISTIO");
        assertThat(antes.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(antes.getBody()).containsEntry("mensaje", "La asistencia se marca desde la hora de llegada de la reserva");

        reloj.fijar(MANIANA.atTime(20, 15));
        ResponseEntity<Map> asistio = cambiarEstado(encargado, id, "ASISTIO");
        assertThat(asistio.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(asistio.getBody()).containsEntry("estado", "ASISTIO");
        assertThat(estado(id)).isEqualTo(EstadoReserva.ASISTIO);

        ResponseEntity<Map> cambiar = cambiarEstado(encargado, id, "NO_ASISTIO");
        assertThat(cambiar.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(cambiar.getBody()).containsEntry("mensaje",
                "La reserva ya está marcada como asistida y no se puede cambiar");

        reloj.fijar(MANIANA.atTime(20, 1).plusHours(48));
        ResponseEntity<Map> tarde = cambiarEstado(encargado, tardia, "NO_ASISTIO");
        assertThat(tarde.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(tarde.getBody()).containsEntry("mensaje",
                "Pasaron más de 48 horas de la reserva: ya no se puede marcar la asistencia");
        assertThat(estado(tardia)).isEqualTo(EstadoReserva.ACEPTADA);
    }

    @Test
    void d4LaAsistenciaSoloSeMarcaEnUnaReservaAceptada() {
        Encargado encargado = api.nuevoEncargado();
        Cliente cliente = api.nuevoCliente();
        Long id = api.crearReserva(cliente, encargado.restaurantId(), MANIANA, 2);
        reloj.fijar(MANIANA.atTime(21, 0));

        ResponseEntity<Map> resp = cambiarEstado(encargado, id, "NO_ASISTIO");

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody()).containsEntry("mensaje", "Solo podés marcar la asistencia de una reserva aceptada");
        assertThat(estado(id)).isEqualTo(EstadoReserva.PENDIENTE);
    }

    @Test
    void d17ReservasSimultaneasNoSuperanElCupo() throws Exception {
        Encargado encargado = api.nuevoEncargado();
        List<Cliente> clientes = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            clientes.add(api.nuevoCliente());
        }
        Long espacio = api.primerEspacio(encargado.restaurantId());
        CountDownLatch largada = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(clientes.size());
        try {
            List<Future<Integer>> resultados = new ArrayList<>();
            for (Cliente cliente : clientes) {
                Callable<Integer> reservar = () -> {
                    largada.await();
                    return api.reservar(cliente, encargado.restaurantId(), espacio, MANIANA, 4).getStatusCode().value();
                };
                resultados.add(pool.submit(reservar));
            }
            largada.countDown();
            long ok = 0;
            for (Future<Integer> resultado : resultados) {
                if (resultado.get() == 200) {
                    ok++;
                }
            }
            // Capacity 10: only two bookings of 4 fit.
            assertThat(ok).isEqualTo(2);
        } finally {
            pool.shutdownNow();
        }
        int personas = reservaRepo.findAllByRestaurant_Id(encargado.restaurantId()).stream()
                .filter(r -> r.getEstadoReserva().esActiva())
                .mapToInt(r -> r.getCantidad())
                .sum();
        assertThat(personas).isEqualTo(8);
    }
}
