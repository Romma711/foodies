package com.example.Foodies;

import com.example.Foodies.AutorizacionTestSupport.Cliente;
import com.example.Foodies.AutorizacionTestSupport.Encargado;
import com.example.Foodies.Restaurant.RestaurantRepository;
import com.example.Foodies.Usuario.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/** S7c (D16): the admin removes strikes giving a reason. S7d: every strike change is kept in a history only the admin sees. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(RelojDePrueba.Config.class)
class StrikesTest {

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
    private UsuarioRepository usuarioRepo;
    @Autowired
    private RelojDePrueba reloj;
    @MockitoBean
    private JavaMailSender mailSender;

    private AutorizacionTestSupport api;
    private String admin;
    private static final LocalDate MANIANA = LocalDate.now().plusDays(1);

    @BeforeEach
    void setUp() {
        reloj.reiniciar();
        api = new AutorizacionTestSupport(rest, restaurantRepo);
        admin = api.admin();
    }

    @AfterEach
    void tearDown() {
        reloj.reiniciar();
    }

    private ResponseEntity<Map> sumar(String quien, Long id, String motivo) {
        return api.send(HttpMethod.POST, "/api/admin/" + quien + "/" + id + "/strikes", admin, Map.of("motivo", motivo), Map.class);
    }

    private ResponseEntity<Map> quitar(String token, String quien, Long id, String motivo) {
        Map<String, Object> body = new HashMap<>();
        body.put("motivo", motivo);
        return api.send(HttpMethod.POST, "/api/admin/" + quien + "/" + id + "/strikes/quitar", token, body, Map.class);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> historial(String quien, Long id) {
        ResponseEntity<List> resp = api.send(HttpMethod.GET, "/api/admin/" + quien + "/" + id + "/strikes", admin, null, List.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        return resp.getBody();
    }

    private int strikesDe(Long usuarioId) {
        return usuarioRepo.findById(usuarioId).orElseThrow().getStrikes();
    }

    @Test
    void elAdminQuitaUnStrikeDeUnClienteConMotivoYQuedaEnElHistorial() {
        Cliente cliente = api.nuevoCliente();
        reloj.fijar(LocalDateTime.of(2026, 11, 20, 10, 30, 15, 123_000_000));
        ResponseEntity<Map> sumado = sumar("usuarios", cliente.id(), "No se presentó a la reserva");
        assertThat(sumado.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(sumado.getBody()).containsEntry("strikes", 1).containsEntry("emailEnviado", true);
        reloj.fijar(LocalDateTime.of(2026, 11, 21, 11, 0));
        sumar("usuarios", cliente.id(), "Maltrató al personal");
        Mockito.clearInvocations(mailSender);

        reloj.fijar(LocalDateTime.of(2026, 11, 22, 9, 45));
        ResponseEntity<Map> quitado = quitar(admin, "usuarios", cliente.id(), "  Era un error de carga  ");

        assertThat(quitado.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(quitado.getBody()).isEqualTo(Map.of("strikes", 1));
        assertThat(strikesDe(cliente.id())).isEqualTo(1);
        verify(mailSender, never()).send(any(SimpleMailMessage.class));

        List<Map<String, Object>> historial = historial("usuarios", cliente.id());
        assertThat(historial).hasSize(3);
        assertThat(historial.get(0)).containsEntry("fecha", "2026-11-22T09:45:00").containsEntry("tipo", "QUITA")
                .containsEntry("origen", "ADMIN").containsEntry("motivo", "Era un error de carga")
                .containsEntry("totalDespues", 1).containsKey("id");
        assertThat(historial.get(1)).containsEntry("fecha", "2026-11-21T11:00:00").containsEntry("tipo", "SUMA")
                .containsEntry("motivo", "Maltrató al personal").containsEntry("totalDespues", 2);
        assertThat(historial.get(2)).containsEntry("fecha", "2026-11-20T10:30:15").containsEntry("tipo", "SUMA")
                .containsEntry("origen", "ADMIN").containsEntry("motivo", "No se presentó a la reserva")
                .containsEntry("totalDespues", 1);
    }

    @Test
    void elAdminQuitaUnStrikeDeUnRestauranteYVeSuHistorial() {
        Encargado encargado = api.nuevoEncargado();
        sumar("restaurantes", encargado.restaurantId(), "Canceló reservas sin aviso");

        ResponseEntity<Map> quitado = quitar(admin, "restaurantes", encargado.restaurantId(), "Lo revisamos y estaba justificado");

        assertThat(quitado.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(quitado.getBody()).isEqualTo(Map.of("strikes", 0));
        assertThat(restaurantRepo.findById(encargado.restaurantId()).orElseThrow().getStrikes()).isZero();
        List<Map<String, Object>> historial = historial("restaurantes", encargado.restaurantId());
        assertThat(historial).extracting(h -> h.get("tipo")).containsExactly("QUITA", "SUMA");
        assertThat(historial).extracting(h -> h.get("totalDespues")).containsExactly(0, 1);
        assertThat(historial.get(1)).containsEntry("origen", "ADMIN").containsEntry("motivo", "Canceló reservas sin aviso");
    }

    @Test
    void quitarSinMotivoRespondeMotivoObligatorio() {
        Cliente cliente = api.nuevoCliente();
        sumar("usuarios", cliente.id(), "No se presentó a la reserva");

        for (String motivo : new String[]{"   ", "", null}) {
            ResponseEntity<Map> resp = quitar(admin, "usuarios", cliente.id(), motivo);
            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(resp.getBody()).containsEntry("codigo", "MOTIVO_OBLIGATORIO")
                    .containsEntry("mensaje", "Indicá el motivo");
        }
        assertThat(strikesDe(cliente.id())).isEqualTo(1);
        assertThat(historial("usuarios", cliente.id())).hasSize(1);
    }

    @Test
    void sinStrikesNoHayNadaQueQuitar() {
        Cliente cliente = api.nuevoCliente();
        Encargado encargado = api.nuevoEncargado();

        ResponseEntity<Map> usuario = quitar(admin, "usuarios", cliente.id(), "Por las dudas");
        ResponseEntity<Map> restaurante = quitar(admin, "restaurantes", encargado.restaurantId(), "Por las dudas");

        for (ResponseEntity<Map> resp : List.of(usuario, restaurante)) {
            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(resp.getBody()).containsEntry("codigo", "SIN_STRIKES")
                    .containsEntry("mensaje", "No tiene strikes para quitar");
        }
        assertThat(strikesDe(cliente.id())).isZero();
        assertThat(historial("usuarios", cliente.id())).isEmpty();
        assertThat(historial("restaurantes", encargado.restaurantId())).isEmpty();
    }

    @Test
    void unIdQueNoExisteResponde404() {
        assertThat(quitar(admin, "usuarios", 999_999L, "Motivo cualquiera").getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(quitar(admin, "restaurantes", 999_999L, "Motivo cualquiera").getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(api.status(HttpMethod.GET, "/api/admin/usuarios/999999/strikes", admin, null)).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(api.status(HttpMethod.GET, "/api/admin/restaurantes/999999/strikes", admin, null)).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void soloElAdminQuitaStrikesYVeElHistorial() {
        Cliente cliente = api.nuevoCliente();
        Encargado encargado = api.nuevoEncargado();
        sumar("usuarios", cliente.id(), "No se presentó a la reserva");
        sumar("restaurantes", encargado.restaurantId(), "Canceló reservas sin aviso");

        for (String token : new String[]{cliente.token(), encargado.token()}) {
            assertThat(quitar(token, "usuarios", cliente.id(), "Me lo saco").getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(quitar(token, "restaurantes", encargado.restaurantId(), "Me lo saco").getStatusCode())
                    .isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(api.status(HttpMethod.GET, "/api/admin/usuarios/" + cliente.id() + "/strikes", token, null))
                    .isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(api.status(HttpMethod.GET, "/api/admin/restaurantes/" + encargado.restaurantId() + "/strikes", token, null))
                    .isEqualTo(HttpStatus.FORBIDDEN);
        }
        assertThat(quitar(null, "usuarios", cliente.id(), "Me lo saco").getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(strikesDe(cliente.id())).isEqualTo(1);
        assertThat(restaurantRepo.findById(encargado.restaurantId()).orElseThrow().getStrikes()).isEqualTo(1);
    }

    @Test
    void elStrikeAutomaticoPorAusenciasQuedaEnElHistorial() {
        Encargado encargado = api.nuevoEncargado();
        Cliente cliente = api.nuevoCliente();

        for (int i = 0; i < 3; i++) {
            LocalDate fecha = MANIANA.plusDays(i);
            reloj.reiniciar();
            Long id = api.crearReserva(cliente, encargado.restaurantId(), fecha, 2);
            api.send(HttpMethod.PATCH, "/api/reservas/" + id + "/estado", encargado.token(), Map.of("estado", "ACEPTADA"), Map.class);
            reloj.fijar(fecha.atTime(21, 0));
            assertThat(api.send(HttpMethod.PATCH, "/api/reservas/" + id + "/estado", encargado.token(),
                    Map.of("estado", "NO_ASISTIO"), Map.class).getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        List<Map<String, Object>> historial = historial("usuarios", cliente.id());
        assertThat(historial).hasSize(1);
        assertThat(historial.get(0)).containsEntry("tipo", "SUMA").containsEntry("origen", "AUSENCIAS")
                .containsEntry("motivo", "No asististe a 3 reservas sin avisar.").containsEntry("totalDespues", 1)
                .containsEntry("fecha", MANIANA.plusDays(2).atTime(21, 0) + ":00");
    }

    @Test
    void elAdminSigueBorrandoCuentasConHistorialDeStrikes() {
        Cliente cliente = api.nuevoCliente();
        Encargado encargado = api.nuevoEncargado();
        sumar("usuarios", cliente.id(), "No se presentó a la reserva");
        sumar("restaurantes", encargado.restaurantId(), "Canceló reservas sin aviso");

        assertThat(api.status(HttpMethod.DELETE, "/api/clientes/" + cliente.id(), admin, null)).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(api.status(HttpMethod.DELETE, "/api/restaurantes/" + encargado.restaurantId(), admin, null))
                .isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(usuarioRepo.findById(cliente.id())).isEmpty();
        assertThat(restaurantRepo.findById(encargado.restaurantId())).isEmpty();
    }

    @Test
    void d6ConMenosDeTresStrikesVuelveAReservar() {
        Encargado encargado = api.nuevoEncargado();
        Cliente cliente = api.nuevoCliente();
        for (int i = 0; i < 3; i++) {
            sumar("usuarios", cliente.id(), "No se presentó a la reserva");
        }
        assertThat(api.reservar(cliente, encargado.restaurantId(), MANIANA, 2).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        assertThat(quitar(admin, "usuarios", cliente.id(), "Revisamos el caso").getBody()).isEqualTo(Map.of("strikes", 2));

        assertThat(api.reservar(cliente, encargado.restaurantId(), MANIANA, 2).getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
