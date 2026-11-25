package com.example.Foodies;

import com.example.Foodies.AutorizacionTestSupport.Cliente;
import com.example.Foodies.AutorizacionTestSupport.Encargado;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** S1: business-rule errors carry a stable {@code codigo} and the values of their message in {@code params}. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(RelojDePrueba.Config.class)
class CodigosDeErrorTest {

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

    @Test
    void lasReglasDeReservaDevuelvenCodigoYParametros() {
        Encargado encargado = api.nuevoEncargado();
        Cliente cliente = api.nuevoCliente();
        Long id = api.crearReserva(cliente, encargado.restaurantId(), MANIANA, 2);

        ResponseEntity<Map> duplicada = api.reservar(cliente, encargado.restaurantId(), MANIANA, 2);
        assertThat(duplicada.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(duplicada.getBody()).containsEntry("codigo", "RESERVA_DUPLICADA").containsEntry("params", Map.of())
                .containsEntry("error", "conflicto").containsKey("mensaje");

        ResponseEntity<Map> sinCupo = api.reservar(api.nuevoCliente(), encargado.restaurantId(), MANIANA, 9);
        assertThat(sinCupo.getBody()).containsEntry("codigo", "SIN_CUPO").containsEntry("params", Map.of("cantidad", 9));

        reloj.fijar(MANIANA.atTime(19, 45));
        ResponseEntity<Map> tarde = api.send(HttpMethod.PUT, "/api/reservas/" + id, cliente.token(),
                Map.of("cantidad", 3), Map.class);
        assertThat(tarde.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(tarde.getBody()).containsEntry("codigo", "PLAZO_VENCIDO").containsEntry("params", Map.of("minutos", 30));
    }

    @Test
    void conTurnosElCupoHablaDelTurno() {
        Encargado encargado = api.nuevoEncargado();
        String dia = MANIANA.getDayOfWeek().name();
        api.send(HttpMethod.POST, "/api/restaurantes/me/turnos", encargado.token(), Map.of("dia", dia, "inicio", "20:00"), Map.class);
        api.crearReserva(api.nuevoCliente(), encargado.restaurantId(), MANIANA, 10);

        ResponseEntity<Map> sinLugar = api.reservar(api.nuevoCliente(), encargado.restaurantId(), MANIANA, 1);
        assertThat(sinLugar.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(sinLugar.getBody())
                .containsEntry("codigo", "SIN_CUPO_TURNO")
                .containsEntry("mensaje", "No hay lugar para 1 persona en ese turno. Probá con otro turno o menos personas.");

        Map<String, Object> body = new HashMap<>();
        body.put("idEspacio", api.primerEspacio(encargado.restaurantId()));
        body.put("cantidad", 2);
        body.put("fechaReserva", MANIANA.toString());
        body.put("horarioLlegada", "21:00");
        body.put("idRestaurant", encargado.restaurantId());
        ResponseEntity<Map> otraHora = api.send(HttpMethod.POST, "/api/reservas", api.nuevoCliente().token(), body, Map.class);
        assertThat(otraHora.getBody()).containsEntry("codigo", "TURNO_INVALIDO")
                .containsEntry("params", Map.of("turnos", List.of("20:00")));
    }

    @Test
    void elHorarioYLosFiltrosTambienDevuelvenCodigo() {
        Encargado encargado = api.nuevoEncargado();
        String dia = MANIANA.getDayOfWeek().name();
        api.send(HttpMethod.PUT, "/api/restaurantes/me/horarios", encargado.token(),
                List.of(Map.of("dia", dia, "apertura", "12:00", "cierre", "16:00")), List.class);
        Long espacio = api.primerEspacio(encargado.restaurantId());

        Map<String, Object> body = new HashMap<>();
        body.put("idEspacio", espacio);
        body.put("cantidad", 2);
        body.put("fechaReserva", MANIANA.toString());
        body.put("horarioLlegada", "15:30");
        body.put("idRestaurant", encargado.restaurantId());
        ResponseEntity<Map> tarde = api.send(HttpMethod.POST, "/api/reservas", api.nuevoCliente().token(), body, Map.class);
        assertThat(tarde.getBody()).containsEntry("codigo", "ULTIMA_LLEGADA")
                .containsEntry("params", Map.of("minutos", 60, "hora", "15:00"));

        body.put("horarioLlegada", "18:00");
        ResponseEntity<Map> fuera = api.send(HttpMethod.POST, "/api/reservas", api.nuevoCliente().token(), body, Map.class);
        assertThat(fuera.getBody()).containsEntry("codigo", "FUERA_DE_HORARIO")
                .containsEntry("params", Map.of("franjas", List.of(Map.of("apertura", "12:00", "cierre", "16:00"))));

        api.send(HttpMethod.PATCH, "/api/admin/restaurantes/" + encargado.restaurantId() + "/activo", api.admin(),
                Map.of("activo", false), Map.class);
        body.put("horarioLlegada", "13:00");
        ResponseEntity<Map> oculto = api.send(HttpMethod.POST, "/api/reservas", api.nuevoCliente().token(), body, Map.class);
        assertThat(oculto.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(oculto.getBody()).containsEntry("codigo", "RESTAURANTE_NO_ENCONTRADO")
                .containsEntry("mensaje", "Restaurante no encontrado");
        ResponseEntity<Map> panel = api.send(HttpMethod.PATCH, "/api/restaurantes/me", encargado.token(),
                Map.of("nombre", "Otro"), Map.class);
        assertThat(panel.getBody()).containsEntry("codigo", "RESTAURANTE_DADO_DE_BAJA");
    }

    @Test
    void losErroresSinCodigoNoCambian() {
        ResponseEntity<Map> resp = api.send(HttpMethod.GET, "/api/restaurantes/999999", null, null, Map.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resp.getBody()).containsOnlyKeys("error", "mensaje");
    }
}
