package com.example.Foodies;

import com.example.Foodies.AutorizacionTestSupport.Cliente;
import com.example.Foodies.AutorizacionTestSupport.Encargado;
import com.example.Foodies.Enums.EstadoReserva;
import com.example.Foodies.Reserva.Reserva;
import com.example.Foodies.Reserva.ReservaRepository;
import com.example.Foodies.Restaurant.RestaurantRepository;
import com.example.Foodies.Usuario.Usuario;
import com.example.Foodies.Usuario.UsuarioRepository;
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
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Client-side reservation rules: duplicates (D1), deadlines (D3), modify (D9), delete (D8), roles (D13), strikes (D6, D16). */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(RelojDePrueba.Config.class)
class ReglasClienteTest {

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
    private UsuarioRepository usuarioRepo;
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

    private ResponseEntity<Map> reservarA(Cliente cliente, Long restaurantId, Long espacioId, LocalDate fecha, String hora) {
        Map<String, Object> body = new HashMap<>();
        body.put("idEspacio", espacioId);
        body.put("cantidad", 2);
        body.put("fechaReserva", fecha.toString());
        body.put("horarioLlegada", hora);
        body.put("idRestaurant", restaurantId);
        return api.send(HttpMethod.POST, "/api/reservas", cliente.token(), body, Map.class);
    }

    private int reservasDe(Cliente cliente) {
        return reservaRepo.findAllByUsuario_Id(cliente.id()).size();
    }

    @Test
    void bl01UnClienteNoReservaDosVecesElMismoRestauranteElMismoDiaNiEnOtroEspacio() {
        Encargado encargado = api.nuevoEncargado();
        Cliente cliente = api.nuevoCliente();
        Long espacio = api.primerEspacio(encargado.restaurantId());
        Long patio = ((Number) api.send(HttpMethod.POST, "/api/restaurantes/me/espacios", encargado.token(),
                Map.of("tipo", "PATIO", "nombre", "Patio", "capacidad", 10), Map.class).getBody().get("id")).longValue();
        api.crearReserva(cliente, encargado.restaurantId(), MANIANA, 2);

        ResponseEntity<Map> mismo = reservarA(cliente, encargado.restaurantId(), espacio, MANIANA, "22:30");
        ResponseEntity<Map> otroEspacio = reservarA(cliente, encargado.restaurantId(), patio, MANIANA, "13:00");

        for (ResponseEntity<Map> resp : java.util.List.of(mismo, otroEspacio)) {
            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(resp.getBody()).containsEntry("mensaje",
                    "Ya tenés una reserva activa en este restaurante ese día. Modificala si necesitás cambiarla.");
        }
        assertThat(reservasDe(cliente)).isEqualTo(1);
        // Another day, or once the first one is cancelled, it is allowed again.
        assertThat(reservarA(cliente, encargado.restaurantId(), espacio, MANIANA.plusDays(1), "20:00").getStatusCode())
                .isEqualTo(HttpStatus.OK);
    }

    @Test
    void bl02DosRestaurantesDistintosNecesitanAlMenosDosHorasEntreReservas() {
        Encargado uno = api.nuevoEncargado();
        Encargado dos = api.nuevoEncargado();
        Cliente cliente = api.nuevoCliente();
        api.crearReserva(cliente, uno.restaurantId(), MANIANA, 2);
        Long espacioDos = api.primerEspacio(dos.restaurantId());

        ResponseEntity<Map> cerca = reservarA(cliente, dos.restaurantId(), espacioDos, MANIANA, "21:30");
        assertThat(cerca.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(cerca.getBody()).containsEntry("mensaje",
                "Ya tenés otra reserva activa a menos de 2 horas de este horario.");
        assertThat(reservasDe(cliente)).isEqualTo(1);

        assertThat(reservarA(cliente, dos.restaurantId(), espacioDos, MANIANA, "22:00").getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(reservasDe(cliente)).isEqualTo(2);
    }

    @Test
    void d1ModificarTambienRevisaLosDuplicados() {
        Encargado encargado = api.nuevoEncargado();
        Cliente cliente = api.nuevoCliente();
        api.crearReserva(cliente, encargado.restaurantId(), MANIANA, 2);
        Long otra = api.crearReserva(cliente, encargado.restaurantId(), MANIANA.plusDays(1), 2);

        ResponseEntity<Map> resp = api.send(HttpMethod.PUT, "/api/reservas/" + otra, cliente.token(),
                Map.of("fechaReserva", MANIANA.toString()), Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(reservaRepo.findById(otra).orElseThrow().fechaLocal()).isEqualTo(MANIANA.plusDays(1));
    }

    @Test
    void d3ElClienteCancelaOModificaHasta30MinutosAntes() {
        Encargado encargado = api.nuevoEncargado();
        Cliente cliente = api.nuevoCliente();
        Long id = api.crearReserva(cliente, encargado.restaurantId(), MANIANA, 2);
        reloj.fijar(MANIANA.atTime(19, 31));
        String mensaje = "Solo podés cancelar o modificar la reserva hasta 30 minutos antes de la llegada. "
                + "Para cambios de último momento, comunicate con el restaurante.";

        ResponseEntity<Map> cancelar = api.send(HttpMethod.PUT, "/api/reservas/" + id, cliente.token(),
                Map.of("estadoReserva", "CANCELADA"), Map.class);
        ResponseEntity<Map> modificar = api.send(HttpMethod.PUT, "/api/reservas/" + id, cliente.token(),
                Map.of("cantidad", 3), Map.class);

        assertThat(cancelar.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(cancelar.getBody()).containsEntry("mensaje", mensaje);
        assertThat(modificar.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(modificar.getBody()).containsEntry("mensaje", mensaje);
        Reserva intacta = reservaRepo.findById(id).orElseThrow();
        assertThat(intacta.getEstadoReserva()).isEqualTo(EstadoReserva.PENDIENTE);
        assertThat(intacta.getCantidad()).isEqualTo(2);

        reloj.fijar(MANIANA.atTime(19, 30));
        assertThat(api.send(HttpMethod.PUT, "/api/reservas/" + id, cliente.token(),
                Map.of("estadoReserva", "CANCELADA"), Map.class).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void d9ModificarUnaAceptadaReevaluaLaAutoAceptacion() {
        Encargado encargado = api.nuevoEncargado();
        api.send(HttpMethod.PATCH, "/api/restaurantes/me", encargado.token(), Map.of("porcentajeAutoAceptacion", 50), Map.class);
        Cliente cliente = api.nuevoCliente();
        ResponseEntity<Map> creada = api.reservar(cliente, encargado.restaurantId(), MANIANA, 2);
        assertThat(creada.getBody()).containsEntry("estadoReserva", "ACEPTADA");
        Long id = ((Number) creada.getBody().get("id")).longValue();

        ResponseEntity<Map> chica = api.send(HttpMethod.PUT, "/api/reservas/" + id, cliente.token(),
                Map.of("cantidad", 4), Map.class);
        assertThat(chica.getBody()).containsEntry("estadoReserva", "ACEPTADA");

        // 6 of 10 is above the 50 % auto-accepted: back to the restaurant.
        ResponseEntity<Map> grande = api.send(HttpMethod.PUT, "/api/reservas/" + id, cliente.token(),
                Map.of("cantidad", 6), Map.class);
        assertThat(grande.getBody()).containsEntry("estadoReserva", "PENDIENTE");
        assertThat(reservaRepo.findById(id).orElseThrow().getEstadoReserva()).isEqualTo(EstadoReserva.PENDIENTE);
    }

    @Test
    void bl13ElClienteNoBorraReservasSoloLasCancela() {
        Encargado encargado = api.nuevoEncargado();
        Cliente cliente = api.nuevoCliente();
        Long id = api.crearReserva(cliente, encargado.restaurantId(), MANIANA, 2);

        ResponseEntity<Map> resp = api.send(HttpMethod.DELETE, "/api/reservas/" + id, cliente.token(), null, Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(resp.getBody()).containsEntry("mensaje", "Las reservas no se borran: cancelala para liberar el lugar");
        assertThat(reservaRepo.findById(id)).isPresent();
    }

    @Test
    void bl23ElAdminNoPuedeReservar() {
        Encargado encargado = api.nuevoEncargado();
        Map<String, Object> body = new HashMap<>();
        body.put("idEspacio", api.primerEspacio(encargado.restaurantId()));
        body.put("cantidad", 2);
        body.put("fechaReserva", MANIANA.toString());
        body.put("horarioLlegada", "20:00");
        body.put("idRestaurant", encargado.restaurantId());

        ResponseEntity<Map> resp = api.send(HttpMethod.POST, "/api/reservas", api.admin(), body, Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(resp.getBody()).containsEntry("mensaje", "Solo los clientes pueden hacer reservas");
        assertThat(reservaRepo.findAllByRestaurant_Id(encargado.restaurantId())).isEmpty();
    }

    @Test
    void bl16ConTresStrikesNoPuedeReservarPeroSiEntrarYVerSusReservas() {
        Encargado encargado = api.nuevoEncargado();
        Cliente cliente = api.nuevoCliente();
        Long previa = api.crearReserva(cliente, encargado.restaurantId(), MANIANA, 2);
        Usuario usuario = usuarioRepo.findById(cliente.id()).orElseThrow();
        usuario.setStrikes(3);
        usuarioRepo.save(usuario);

        ResponseEntity<Map> resp = api.reservar(cliente, encargado.restaurantId(), MANIANA.plusDays(2), 2);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(resp.getBody()).containsEntry("mensaje",
                "Tu cuenta tiene 3 strikes: no podés hacer reservas nuevas. Escribinos si creés que es un error.");
        assertThat(reservasDe(cliente)).isEqualTo(1);
        assertThat(api.login(cliente.email(), "secret1")).isNotBlank();
        assertThat(api.status(HttpMethod.GET, "/api/reservas/" + previa, cliente.token(), null)).isEqualTo(HttpStatus.OK);
    }

    @Test
    void bl17NoSeSancionaUnaCuentaDadaDeBaja() {
        Cliente cliente = api.nuevoCliente();
        Encargado encargado = api.nuevoEncargado();
        String admin = api.admin();
        api.send(HttpMethod.PATCH, "/api/admin/usuarios/" + cliente.id() + "/activo", admin, Map.of("activo", false), Map.class);
        api.send(HttpMethod.PATCH, "/api/admin/restaurantes/" + encargado.restaurantId() + "/activo", admin,
                Map.of("activo", false), Map.class);

        ResponseEntity<Map> usuario = api.send(HttpMethod.POST, "/api/admin/usuarios/" + cliente.id() + "/strikes", admin,
                Map.of("motivo", "No se presentó"), Map.class);
        ResponseEntity<Map> resto = api.send(HttpMethod.POST, "/api/admin/restaurantes/" + encargado.restaurantId()
                + "/strikes", admin, Map.of("motivo", "No se presentó"), Map.class);

        for (ResponseEntity<Map> resp : java.util.List.of(usuario, resto)) {
            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(resp.getBody()).containsEntry("mensaje", "No se puede sancionar una cuenta dada de baja");
        }
        assertThat(usuarioRepo.findById(cliente.id()).orElseThrow().getStrikes()).isZero();
        assertThat(restaurantRepo.findById(encargado.restaurantId()).orElseThrow().getStrikes()).isZero();
    }
}
