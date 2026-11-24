package com.example.Foodies;

import com.example.Foodies.AutorizacionTestSupport.Cliente;
import com.example.Foodies.AutorizacionTestSupport.Encargado;
import com.example.Foodies.Reserva.Reserva;
import com.example.Foodies.Reserva.ReservaRepository;
import com.example.Foodies.Restaurant.RestaurantRepository;
import com.example.Foodies.Turno.TurnoRepository;
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
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** RF24 / D2: shifts with capacity per shift, managed from the panel. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TurnosTest {

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
    private TurnoRepository turnoRepo;

    private AutorizacionTestSupport api;
    private static final LocalDate FECHA = LocalDate.now().plusDays(2);
    private static final String DIA = FECHA.getDayOfWeek().name();

    @BeforeEach
    void setUp() {
        api = new AutorizacionTestSupport(rest, restaurantRepo);
    }

    private ResponseEntity<Map> crearTurno(Encargado encargado, String dia, String inicio, Integer duracion) {
        Map<String, Object> body = new HashMap<>();
        body.put("dia", dia);
        body.put("inicio", inicio);
        if (duracion != null) {
            body.put("duracionMinutos", duracion);
        }
        return api.send(HttpMethod.POST, "/api/restaurantes/me/turnos", encargado.token(), body, Map.class);
    }

    private Long idTurno(ResponseEntity<Map> resp) {
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return ((Number) resp.getBody().get("id")).longValue();
    }

    private ResponseEntity<Map> reservar(Cliente cliente, Long restaurantId, Long idTurno, String hora, int cantidad) {
        Map<String, Object> body = new HashMap<>();
        body.put("idEspacio", api.primerEspacio(restaurantId));
        body.put("cantidad", cantidad);
        body.put("fechaReserva", FECHA.toString());
        body.put("horarioLlegada", hora);
        body.put("idRestaurant", restaurantId);
        if (idTurno != null) {
            body.put("idTurno", idTurno);
        }
        return api.send(HttpMethod.POST, "/api/reservas", cliente.token(), body, Map.class);
    }

    @Test
    void elEncargadoCreaListaModificaYQuitaTurnos() {
        Encargado encargado = api.nuevoEncargado();

        ResponseEntity<Map> creado = crearTurno(encargado, "SATURDAY", "20:00", null);
        assertThat(creado.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(creado.getBody()).containsEntry("dia", "SATURDAY").containsEntry("inicio", "20:00")
                .containsEntry("fin", "21:30").containsEntry("duracionMinutos", 90);
        Long id = idTurno(creado);

        ResponseEntity<Map> repetido = crearTurno(encargado, "SATURDAY", "20:00", 60);
        assertThat(repetido.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(repetido.getBody()).containsEntry("mensaje", "Ya tenés un turno ese día a esa hora");
        ResponseEntity<Map> medianoche = crearTurno(encargado, "SATURDAY", "23:30", 60);
        assertThat(medianoche.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(medianoche.getBody()).containsEntry("mensaje", "El turno tiene que terminar a la medianoche como máximo");

        ResponseEntity<Map> cambiado = api.send(HttpMethod.PUT, "/api/restaurantes/me/turnos/" + id, encargado.token(),
                Map.of("dia", "SATURDAY", "inicio", "21:00", "duracionMinutos", 120), Map.class);
        assertThat(cambiado.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(cambiado.getBody()).containsEntry("fin", "23:00");

        ResponseEntity<List> publico = rest.getForEntity("/api/restaurantes/{id}/turnos", List.class, encargado.restaurantId());
        assertThat(publico.getBody()).hasSize(1);

        assertThat(api.status(HttpMethod.DELETE, "/api/restaurantes/me/turnos/" + id, encargado.token(), null))
                .isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(rest.getForEntity("/api/restaurantes/{id}/turnos", List.class, encargado.restaurantId()).getBody())
                .isEmpty();
        assertThat(turnoRepo.findById(id)).hasValueSatisfying(t -> assertThat(t.getActivo()).isFalse());
    }

    @Test
    void soloElEncargadoAdministraTurnos() {
        Cliente cliente = api.nuevoCliente();
        assertThat(api.status(HttpMethod.POST, "/api/restaurantes/me/turnos", cliente.token(),
                Map.of("dia", "MONDAY", "inicio", "20:00"))).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(api.status(HttpMethod.POST, "/api/restaurantes/me/turnos", null,
                Map.of("dia", "MONDAY", "inicio", "20:00"))).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void d11ElTurnoTieneQueEntrarEnElHorarioDeAtencion() {
        Encargado encargado = api.nuevoEncargado();
        api.send(HttpMethod.PUT, "/api/restaurantes/me/horarios", encargado.token(),
                List.of(Map.of("dia", "FRIDAY", "apertura", "20:00", "cierre", "23:00")), List.class);

        ResponseEntity<Map> tarde = crearTurno(encargado, "FRIDAY", "22:00", 90);
        assertThat(tarde.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(tarde.getBody()).containsEntry("mensaje",
                "El turno tiene que empezar y terminar dentro del horario de atención de ese día");
        assertThat(crearTurno(encargado, "FRIDAY", "21:30", 90).getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void d2ElCupoRigePorTurno() {
        Encargado encargado = api.nuevoEncargado();
        Long primero = idTurno(crearTurno(encargado, DIA, "20:00", null));
        Long segundo = idTurno(crearTurno(encargado, DIA, "21:30", null));

        ResponseEntity<Map> llena = reservar(api.nuevoCliente(), encargado.restaurantId(), primero, "20:00", 10);
        assertThat(llena.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(llena.getBody()).containsEntry("idTurno", primero.intValue()).containsEntry("horariollegada", "20:00:00");
        // Same day, another shift: its own capacity of 10.
        ResponseEntity<Map> otra = reservar(api.nuevoCliente(), encargado.restaurantId(), null, "21:30", 10);
        assertThat(otra.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(otra.getBody()).containsEntry("idTurno", segundo.intValue());

        ResponseEntity<Map> sinLugar = reservar(api.nuevoCliente(), encargado.restaurantId(), primero, "20:00", 1);
        assertThat(sinLugar.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Reserva guardada = reservaRepo.findById(((Number) llena.getBody().get("id")).longValue()).orElseThrow();
        assertThat(guardada.getTurno().getId()).isEqualTo(primero);
        assertThat(guardada.getHorariollegada()).isEqualTo(LocalTime.of(20, 0));
    }

    @Test
    void conTurnosLaHoraTieneQueSerElInicioDeUnTurno() {
        Encargado encargado = api.nuevoEncargado();
        crearTurno(encargado, DIA, "20:00", null);
        crearTurno(encargado, DIA, "21:30", null);

        ResponseEntity<Map> resp = reservar(api.nuevoCliente(), encargado.restaurantId(), null, "20:45", 2);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody()).containsEntry("mensaje",
                "Ese día el restaurante recibe reservas por turnos: 20:00, 21:30. Elegí uno.");
        assertThat(reservaRepo.findAllByRestaurant_Id(encargado.restaurantId())).isEmpty();
    }

    @Test
    void laDisponibilidadMuestraLosLugaresLibresPorTurnoYEspacio() {
        Encargado encargado = api.nuevoEncargado();
        Encargado sinTurnos = api.nuevoEncargado();
        Long primero = idTurno(crearTurno(encargado, DIA, "20:00", null));
        idTurno(crearTurno(encargado, DIA, "21:30", null));
        reservar(api.nuevoCliente(), encargado.restaurantId(), primero, "20:00", 4);

        ResponseEntity<Map> resp = rest.getForEntity("/api/restaurantes/{id}/disponibilidad?fecha=" + FECHA, Map.class,
                encargado.restaurantId());

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).containsEntry("porTurnos", true);
        List<Map<String, Object>> turnos = (List<Map<String, Object>>) resp.getBody().get("turnos");
        assertThat(turnos).extracting(t -> t.get("inicio")).containsExactly("20:00", "21:30");
        assertThat(turnos.stream()
                .map(t -> (Object) ((Map<?, ?>) ((List<?>) t.get("espacios")).get(0)).get("libres"))
                .toList()).containsExactly(6, 10);

        ResponseEntity<Map> porDia = rest.getForEntity("/api/restaurantes/{id}/disponibilidad?fecha=" + FECHA, Map.class,
                sinTurnos.restaurantId());
        assertThat(porDia.getBody()).containsEntry("porTurnos", false);
        List<Map<String, Object>> dia = (List<Map<String, Object>>) porDia.getBody().get("turnos");
        assertThat(dia).hasSize(1);
        assertThat(dia.get(0).get("idTurno")).isNull();
    }

    @Test
    void unTurnoConReservasFuturasNoSeBorra() {
        Encargado encargado = api.nuevoEncargado();
        Long turno = idTurno(crearTurno(encargado, DIA, "20:00", null));
        reservar(api.nuevoCliente(), encargado.restaurantId(), turno, "20:00", 2);

        ResponseEntity<Map> resp = api.send(HttpMethod.DELETE, "/api/restaurantes/me/turnos/" + turno,
                encargado.token(), null, Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(turnoRepo.findById(turno)).hasValueSatisfying(t -> assertThat(t.getActivo()).isTrue());
    }
}
