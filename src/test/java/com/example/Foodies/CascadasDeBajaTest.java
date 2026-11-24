package com.example.Foodies;

import com.example.Foodies.AutorizacionTestSupport.Cliente;
import com.example.Foodies.AutorizacionTestSupport.Encargado;
import com.example.Foodies.Enums.EstadoReserva;
import com.example.Foodies.Reserva.ReservaRepository;
import com.example.Foodies.Restaurant.Restaurant;
import com.example.Foodies.Restaurant.RestaurantRepository;
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
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** D7: deactivations cancel future active reservations; deactivated restaurants leave the catalog and lock the panel. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(RelojDePrueba.Config.class)
class CascadasDeBajaTest {

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

    private EstadoReserva estado(Long id) {
        return reservaRepo.findById(id).orElseThrow().getEstadoReserva();
    }

    private ResponseEntity<Map> activo(String tipo, Long id, boolean activo) {
        return api.send(HttpMethod.PATCH, "/api/admin/" + tipo + "/" + id + "/activo", api.admin(),
                Map.of("activo", activo), Map.class);
    }

    private Long aceptar(Encargado encargado, Long reservaId) {
        assertThat(api.send(HttpMethod.PATCH, "/api/reservas/" + reservaId + "/estado", encargado.token(),
                Map.of("estado", "ACEPTADA"), Map.class).getStatusCode()).isEqualTo(HttpStatus.OK);
        return reservaId;
    }

    @Test
    void bl08DarDeBajaUnClienteCancelaSusReservasFuturasYLiberaElCupo() {
        Encargado encargado = api.nuevoEncargado();
        Cliente cliente = api.nuevoCliente();
        Long hoy = aceptar(encargado, api.crearReserva(cliente, encargado.restaurantId(), MANIANA, 2));
        Long futura = api.crearReserva(cliente, encargado.restaurantId(), MANIANA.plusDays(2), 10);
        // "Now" is after the first arrival: that one already happened and must stay as it was.
        reloj.fijar(MANIANA.atTime(21, 0));

        assertThat(activo("usuarios", cliente.id(), false).getStatusCode()).isEqualTo(HttpStatus.OK);

        assertThat(estado(futura)).isEqualTo(EstadoReserva.CANCELADA);
        assertThat(estado(hoy)).isEqualTo(EstadoReserva.ACEPTADA);
        assertThat(api.reservar(api.nuevoCliente(), encargado.restaurantId(), MANIANA.plusDays(2), 10).getStatusCode())
                .isEqualTo(HttpStatus.OK);
    }

    @Test
    void bl09DarDeBajaUnRestauranteCancelaSusReservasYBloqueaElPanel() {
        Encargado encargado = api.nuevoEncargado();
        Cliente cliente = api.nuevoCliente();
        Long reserva = aceptar(encargado, api.crearReserva(cliente, encargado.restaurantId(), MANIANA, 2));
        Long pendiente = api.crearReserva(api.nuevoCliente(), encargado.restaurantId(), MANIANA, 2);

        assertThat(activo("restaurantes", encargado.restaurantId(), false).getStatusCode()).isEqualTo(HttpStatus.OK);

        assertThat(estado(reserva)).isEqualTo(EstadoReserva.CANCELADA);
        assertThat(estado(pendiente)).isEqualTo(EstadoReserva.CANCELADA);
        String mensaje = "Tu restaurante fue dado de baja: el panel queda en modo lectura. "
                + "Escribinos si creés que es un error.";
        ResponseEntity<Map> patch = api.send(HttpMethod.PATCH, "/api/restaurantes/me", encargado.token(),
                Map.of("nombre", "Otro"), Map.class);
        assertThat(patch.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(patch.getBody()).containsEntry("mensaje", mensaje);
        ResponseEntity<Map> put = api.send(HttpMethod.PUT, "/api/restaurantes/me/horarios", encargado.token(),
                java.util.List.of(), Map.class);
        assertThat(put.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        ResponseEntity<Map> estado = api.send(HttpMethod.PATCH, "/api/reservas/" + pendiente + "/estado",
                encargado.token(), Map.of("estado", "ACEPTADA"), Map.class);
        assertThat(estado.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(api.status(HttpMethod.GET, "/api/restaurantes/me", encargado.token(), null)).isEqualTo(HttpStatus.OK);
        Restaurant guardado = restaurantRepo.findById(encargado.restaurantId()).orElseThrow();
        assertThat(guardado.getNombre()).doesNotContain("Otro");
        assertThat(api.send(HttpMethod.PUT, "/api/reservas/" + reserva, cliente.token(), Map.of("cantidad", 3), Map.class)
                .getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void bl10DarDeBajaAlEncargadoCancelaLasReservasYSacaAlRestauranteDelCatalogo() {
        Encargado encargado = api.nuevoEncargado();
        Long reserva = api.crearReserva(api.nuevoCliente(), encargado.restaurantId(), MANIANA, 2);
        Long encargadoId = restaurantRepo.findById(encargado.restaurantId()).orElseThrow().getUsuario().getId();
        Long espacio = api.primerEspacio(encargado.restaurantId());

        assertThat(activo("usuarios", encargadoId, false).getStatusCode()).isEqualTo(HttpStatus.OK);

        assertThat(estado(reserva)).isEqualTo(EstadoReserva.CANCELADA);
        assertThat(api.status(HttpMethod.GET, "/api/restaurantes/" + encargado.restaurantId(), null, null))
                .isEqualTo(HttpStatus.NOT_FOUND);
        ResponseEntity<Map> nueva = api.reservar(api.nuevoCliente(), encargado.restaurantId(), espacio, MANIANA, 2);
        assertThat(nueva.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(nueva.getBody()).containsEntry("mensaje", "Restaurante no encontrado");
    }

    @Test
    void bl11NoSeBorraUnRestauranteConReservasActivasAFuturo() {
        Encargado encargado = api.nuevoEncargado();
        Long reserva = api.crearReserva(api.nuevoCliente(), encargado.restaurantId(), MANIANA, 2);

        ResponseEntity<Map> borrar = api.send(HttpMethod.DELETE, "/api/restaurantes/" + encargado.restaurantId(),
                api.admin(), null, Map.class);

        assertThat(borrar.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(borrar.getBody()).containsEntry("mensaje", "El restaurante tiene reservas activas a futuro: "
                + "dalo de baja primero, así se cancelan y se avisa a los clientes.");
        assertThat(restaurantRepo.findById(encargado.restaurantId())).isPresent();
        assertThat(estado(reserva)).isEqualTo(EstadoReserva.PENDIENTE);

        activo("restaurantes", encargado.restaurantId(), false);
        assertThat(api.status(HttpMethod.DELETE, "/api/restaurantes/" + encargado.restaurantId(), api.admin(), null))
                .isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void bl12ElClienteQueBorraSuCuentaQuedaDadoDeBajaYSusReservasCanceladas() {
        Encargado encargado = api.nuevoEncargado();
        Cliente cliente = api.nuevoCliente();
        Long reserva = api.crearReserva(cliente, encargado.restaurantId(), MANIANA, 2);

        assertThat(api.status(HttpMethod.DELETE, "/api/clientes/" + cliente.id(), cliente.token(), null))
                .isEqualTo(HttpStatus.NO_CONTENT);

        assertThat(usuarioRepo.findById(cliente.id())).hasValueSatisfying(u -> assertThat(u.getActivo()).isFalse());
        assertThat(estado(reserva)).isEqualTo(EstadoReserva.CANCELADA);
        ResponseEntity<Map> login = rest.postForEntity("/api/auth/login",
                Map.of("email", cliente.email(), "password", "secret1"), Map.class);
        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void bl12ElAdminNoBorraFisicamenteUnClienteConReservas() {
        Encargado encargado = api.nuevoEncargado();
        Cliente cliente = api.nuevoCliente();
        api.crearReserva(cliente, encargado.restaurantId(), MANIANA, 2);

        ResponseEntity<Map> borrar = api.send(HttpMethod.DELETE, "/api/clientes/" + cliente.id(), api.admin(), null, Map.class);

        assertThat(borrar.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(borrar.getBody()).containsEntry("mensaje",
                "El cliente tiene reservas o reseñas: dalo de baja en lugar de borrarlo.");
        assertThat(usuarioRepo.findById(cliente.id())).isPresent();
    }
}
