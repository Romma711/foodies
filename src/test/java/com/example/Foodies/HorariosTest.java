package com.example.Foodies;

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
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Horario de atención: franjas por día de la semana que limitan la hora de llegada de las reservas. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class HorariosTest {

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

    private AutorizacionTestSupport api;

    @BeforeEach
    void setUp() {
        api = new AutorizacionTestSupport(rest, restaurantRepo);
    }

    /** Martes a domingo cena 20:00-00:00; sábado y domingo también almuerzo 12:00-16:00. Lunes cerrado. */
    private static List<Map<String, String>> semana() {
        List<Map<String, String>> franjas = new java.util.ArrayList<>();
        for (String dia : List.of("TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY")) {
            franjas.add(Map.of("dia", dia, "apertura", "20:00", "cierre", "00:00"));
        }
        franjas.add(Map.of("dia", "SATURDAY", "apertura", "12:00", "cierre", "16:00"));
        franjas.add(Map.of("dia", "SUNDAY", "apertura", "12:00", "cierre", "16:00"));
        return franjas;
    }

    private static LocalDate proximo(DayOfWeek dia) {
        return LocalDate.now().with(TemporalAdjusters.next(dia));
    }

    private ResponseEntity<Map> reservarA(AutorizacionTestSupport.Cliente cliente, Long restaurantId, LocalDate fecha,
                                          String hora) {
        Map<String, Object> body = new HashMap<>();
        body.put("cantidad", 2);
        body.put("fechaReserva", fecha.toString());
        body.put("horarioLlegada", hora);
        body.put("idRestaurant", restaurantId);
        body.put("idEspacio", api.primerEspacio(restaurantId));
        return api.send(HttpMethod.POST, "/api/reservas", cliente.token(), body, Map.class);
    }

    @Test
    void elEncargadoCargaSuSemanaYSeVePublicamente() {
        AutorizacionTestSupport.Encargado encargado = api.nuevoEncargado();

        ResponseEntity<List> vacio = api.send(HttpMethod.GET, "/api/restaurantes/me/horarios", encargado.token(), null, List.class);
        assertThat(vacio.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(vacio.getBody()).isEmpty();

        ResponseEntity<List> guardado = api.send(HttpMethod.PUT, "/api/restaurantes/me/horarios", encargado.token(), semana(), List.class);
        assertThat(guardado.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(guardado.getBody()).hasSize(8);
        @SuppressWarnings("unchecked")
        Map<String, Object> primera = (Map<String, Object>) guardado.getBody().get(0);
        assertThat(primera).containsEntry("dia", "TUESDAY").containsEntry("apertura", "20:00").containsEntry("cierre", "00:00");

        ResponseEntity<List> publico = rest.getForEntity("/api/restaurantes/{id}/horarios", List.class, encargado.restaurantId());
        assertThat(publico.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(publico.getBody()).hasSize(8);
        // Sábado: primero el almuerzo, después la cena.
        assertThat(publico.getBody()).extracting(f -> ((Map<?, ?>) f).get("dia") + " " + ((Map<?, ?>) f).get("apertura"))
                .containsSubsequence("SATURDAY 12:00", "SATURDAY 20:00");

        // Guardar de nuevo reemplaza toda la semana.
        api.send(HttpMethod.PUT, "/api/restaurantes/me/horarios", encargado.token(),
                List.of(Map.of("dia", "MONDAY", "apertura", "09:00", "cierre", "18:00")), List.class);
        assertThat(rest.getForEntity("/api/restaurantes/{id}/horarios", List.class, encargado.restaurantId()).getBody()).hasSize(1);
    }

    @Test
    void lasFranjasSeValidan() {
        AutorizacionTestSupport.Encargado encargado = api.nuevoEncargado();
        String path = "/api/restaurantes/me/horarios";

        assertThat(mensaje(api.send(HttpMethod.PUT, path, encargado.token(),
                List.of(Map.of("dia", "MONDAY", "apertura", "18:00", "cierre", "12:00")), Map.class)))
                .contains("cierre");
        assertThat(mensaje(api.send(HttpMethod.PUT, path, encargado.token(), List.of(
                Map.of("dia", "MONDAY", "apertura", "12:00", "cierre", "16:00"),
                Map.of("dia", "MONDAY", "apertura", "15:00", "cierre", "18:00")), Map.class)))
                .contains("se superponen");
        assertThat(mensaje(api.send(HttpMethod.PUT, path, encargado.token(), List.of(
                Map.of("dia", "MONDAY", "apertura", "08:00", "cierre", "10:00"),
                Map.of("dia", "MONDAY", "apertura", "11:00", "cierre", "13:00"),
                Map.of("dia", "MONDAY", "apertura", "14:00", "cierre", "16:00"),
                Map.of("dia", "MONDAY", "apertura", "17:00", "cierre", "19:00")), Map.class)))
                .contains("3 franjas");
        assertThat(mensaje(api.send(HttpMethod.PUT, path, encargado.token(),
                List.of(Map.of("dia", "MONDAY", "apertura", "9", "cierre", "18:00")), Map.class)))
                .contains("HH:mm");
        assertThat(mensaje(api.send(HttpMethod.PUT, path, encargado.token(),
                List.of(Map.of("apertura", "09:00", "cierre", "18:00")), Map.class)))
                .contains("día");
        assertThat(api.send(HttpMethod.GET, path, encargado.token(), null, List.class).getBody()).isEmpty();
    }

    @Test
    void soloElEncargadoEditaSusHorarios() {
        AutorizacionTestSupport.Cliente cliente = api.nuevoCliente();
        assertThat(api.status(HttpMethod.PUT, "/api/restaurantes/me/horarios", null, semana())).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(api.status(HttpMethod.PUT, "/api/restaurantes/me/horarios", cliente.token(), semana())).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(api.status(HttpMethod.GET, "/api/restaurantes/999999/horarios", null, null)).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void laReservaTieneQueCaerDentroDelHorarioDeEseDia() {
        AutorizacionTestSupport.Encargado encargado = api.nuevoEncargado();
        AutorizacionTestSupport.Cliente cliente = api.nuevoCliente();
        api.send(HttpMethod.PUT, "/api/restaurantes/me/horarios", encargado.token(), semana(), List.class);
        Long id = encargado.restaurantId();

        assertThat(mensaje(reservarA(cliente, id, proximo(DayOfWeek.MONDAY), "21:00")))
                .isEqualTo("Ese día el restaurante está cerrado. Elegí otra fecha.");
        assertThat(mensaje(reservarA(cliente, id, proximo(DayOfWeek.TUESDAY), "13:00")))
                .isEqualTo("Ese día el restaurante recibe reservas de 20:00 a 00:00. Elegí otro horario.");
        assertThat(mensaje(reservarA(cliente, id, proximo(DayOfWeek.SATURDAY), "17:00")))
                .isEqualTo("Ese día el restaurante recibe reservas de 12:00 a 16:00 y de 20:00 a 00:00. Elegí otro horario.");
        // El horario de cierre ya no recibe llegadas.
        assertThat(reservarA(cliente, id, proximo(DayOfWeek.SATURDAY), "16:00").getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        assertThat(reservarA(cliente, id, proximo(DayOfWeek.SATURDAY), "12:00").getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(reservarA(cliente, id, proximo(DayOfWeek.TUESDAY), "23:30").getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void alModificarLaReservaTambienSeValidaElHorario() {
        AutorizacionTestSupport.Encargado encargado = api.nuevoEncargado();
        AutorizacionTestSupport.Cliente cliente = api.nuevoCliente();
        api.send(HttpMethod.PUT, "/api/restaurantes/me/horarios", encargado.token(), semana(), List.class);
        ResponseEntity<Map> creada = reservarA(cliente, encargado.restaurantId(), proximo(DayOfWeek.TUESDAY), "21:00");
        String path = "/api/reservas/" + ((Number) creada.getBody().get("id")).longValue();

        assertThat(mensaje(api.send(HttpMethod.PUT, path, cliente.token(),
                Map.of("fechaReserva", proximo(DayOfWeek.MONDAY).toString()), Map.class)))
                .contains("cerrado");
        assertThat(mensaje(api.send(HttpMethod.PUT, path, cliente.token(), Map.of("horarioLlegada", "18:00"), Map.class)))
                .contains("de 20:00 a 00:00");
        assertThat(api.send(HttpMethod.PUT, path, cliente.token(), Map.of("horarioLlegada", "22:00"), Map.class)
                .getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void sinHorarioCargadoNoHayRestriccion() {
        AutorizacionTestSupport.Encargado encargado = api.nuevoEncargado();
        AutorizacionTestSupport.Cliente cliente = api.nuevoCliente();

        assertThat(reservarA(cliente, encargado.restaurantId(), proximo(DayOfWeek.MONDAY), "12:00").getStatusCode())
                .isEqualTo(HttpStatus.OK);
    }

    private static String mensaje(ResponseEntity<Map> resp) {
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Object mensaje = resp.getBody() == null ? null : resp.getBody().get("mensaje");
        return mensaje == null ? String.valueOf(resp.getBody()) : mensaje.toString();
    }
}
