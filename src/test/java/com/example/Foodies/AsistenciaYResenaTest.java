package com.example.Foodies;

import com.example.Foodies.AutorizacionTestSupport.Cliente;
import com.example.Foodies.AutorizacionTestSupport.Encargado;
import com.example.Foodies.Resena.ResenaRepository;
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
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** RF26: attendance marked by the restaurant, automatic strike for no-shows (D5) and verified reviews (D12). */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(RelojDePrueba.Config.class)
class AsistenciaYResenaTest {

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
    private ResenaRepository resenaRepo;
    @Autowired
    private RelojDePrueba reloj;
    @MockitoBean
    private JavaMailSender mailSender;

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

    private void cambiarEstado(Encargado encargado, Long reservaId, String estado) {
        ResponseEntity<Map> resp = api.send(HttpMethod.PATCH, "/api/reservas/" + reservaId + "/estado", encargado.token(),
                Map.of("estado", estado), Map.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    /** Books, gets accepted and has the restaurant mark the attendance once the arrival time passed. */
    private void asistencia(Encargado encargado, Cliente cliente, LocalDate fecha, String estado) {
        reloj.reiniciar();
        Long id = api.crearReserva(cliente, encargado.restaurantId(), fecha, 2);
        cambiarEstado(encargado, id, "ACEPTADA");
        reloj.fijar(fecha.atTime(21, 0));
        cambiarEstado(encargado, id, estado);
        reloj.reiniciar();
    }

    private ResponseEntity<Map> resenar(Cliente cliente, Long restaurantId) {
        return api.send(HttpMethod.POST, "/api/resenas", cliente.token(), Map.of(
                "comentario", "Muy rica la comida, volvería.",
                "calificacion", 5,
                "restaurantId", restaurantId), Map.class);
    }

    @Test
    void d5TresAusenciasSumanUnStrikeYReinicianLaCuenta() {
        Encargado encargado = api.nuevoEncargado();
        Cliente cliente = api.nuevoCliente();

        asistencia(encargado, cliente, MANIANA, "NO_ASISTIO");
        asistencia(encargado, cliente, MANIANA.plusDays(1), "NO_ASISTIO");
        Usuario dosAusencias = usuarioRepo.findById(cliente.id()).orElseThrow();
        assertThat(dosAusencias.getStrikes()).isZero();
        assertThat(dosAusencias.getAusencias()).isEqualTo(2);

        asistencia(encargado, cliente, MANIANA.plusDays(2), "NO_ASISTIO");

        Usuario sancionado = usuarioRepo.findById(cliente.id()).orElseThrow();
        assertThat(sancionado.getStrikes()).isEqualTo(1);
        assertThat(sancionado.getAusencias()).isZero();
    }

    @Test
    void bl14SinAsistenciaNoSePuedeResenar() {
        Encargado encargado = api.nuevoEncargado();
        Cliente cliente = api.nuevoCliente();
        asistencia(encargado, cliente, MANIANA, "NO_ASISTIO");

        ResponseEntity<Map> resp = resenar(cliente, encargado.restaurantId());

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(resp.getBody()).containsEntry("mensaje",
                "Solo podés reseñar un restaurante después de asistir a una reserva en él");
        assertThat(resenaRepo.findByUsuario_Id(cliente.id())).isEmpty();
    }

    @Test
    void d12ConAsistenciaSePuedeResenarUnaVez() {
        Encargado encargado = api.nuevoEncargado();
        Cliente cliente = api.nuevoCliente();
        asistencia(encargado, cliente, MANIANA, "ASISTIO");

        assertThat(resenar(cliente, encargado.restaurantId()).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resenar(cliente, encargado.restaurantId()).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resenaRepo.findByUsuario_Id(cliente.id())).hasSize(1);
    }

    @Test
    void bl15NoSeReseniaUnRestauranteDadoDeBaja() {
        Encargado encargado = api.nuevoEncargado();
        Cliente cliente = api.nuevoCliente();
        asistencia(encargado, cliente, MANIANA, "ASISTIO");
        api.send(HttpMethod.PATCH, "/api/admin/restaurantes/" + encargado.restaurantId() + "/activo", api.admin(),
                Map.of("activo", false), Map.class);

        ResponseEntity<Map> resp = resenar(cliente, encargado.restaurantId());

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resp.getBody()).containsEntry("mensaje", "Restaurante no encontrado");
        assertThat(resenaRepo.findByUsuario_Id(cliente.id())).isEmpty();
    }
}
