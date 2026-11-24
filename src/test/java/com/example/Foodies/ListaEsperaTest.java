package com.example.Foodies;

import com.example.Foodies.AutorizacionTestSupport.Cliente;
import com.example.Foodies.AutorizacionTestSupport.Encargado;
import com.example.Foodies.ListaEspera.EstadoListaEspera;
import com.example.Foodies.ListaEspera.ListaEsperaRepository;
import com.example.Foodies.ListaEspera.ListaEsperaService;
import com.example.Foodies.Restaurant.RestaurantRepository;
import com.example.Foodies.Usuario.Usuario;
import com.example.Foodies.Usuario.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

/** RF25 / D15: waitlist when the slot is full; the first entry that fits hears when a spot frees up. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(RelojDePrueba.Config.class)
class ListaEsperaTest {

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
    private ListaEsperaRepository listaRepo;
    @Autowired
    private ListaEsperaService listaEsperaService;
    @Autowired
    private RelojDePrueba reloj;
    @MockitoBean
    private JavaMailSender mailSender;

    private AutorizacionTestSupport api;
    private static final LocalDate FECHA = LocalDate.now().plusDays(2);

    @BeforeEach
    void setUp() {
        reloj.reiniciar();
        reset(mailSender);
        api = new AutorizacionTestSupport(rest, restaurantRepo);
    }

    @AfterEach
    void tearDown() {
        reloj.reiniciar();
    }

    private ResponseEntity<Map> anotar(Cliente cliente, Long restaurantId, int cantidad) {
        Map<String, Object> body = new HashMap<>();
        body.put("idRestaurant", restaurantId);
        body.put("fechaReserva", FECHA.toString());
        body.put("horarioLlegada", "20:00");
        body.put("cantidad", cantidad);
        return api.send(HttpMethod.POST, "/api/lista-espera", cliente.token(), body, Map.class);
    }

    private Long id(ResponseEntity<Map> resp) {
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return ((Number) resp.getBody().get("id")).longValue();
    }

    private EstadoListaEspera estado(Long id) {
        return listaRepo.findById(id).orElseThrow().getEstado();
    }

    private List<SimpleMailMessage> mailsPara(String email) {
        if (mockingDetails(mailSender).getInvocations().isEmpty()) {
            return List.of();
        }
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, atLeast(0)).send(captor.capture());
        return captor.getAllValues().stream()
                .filter(m -> m.getTo() != null && Arrays.asList(m.getTo()).contains(email))
                .toList();
    }

    @Test
    void soloSeAnotaCuandoNoHayLugarYUnaVezPorDia() {
        Encargado encargado = api.nuevoEncargado();
        Cliente cliente = api.nuevoCliente();

        ResponseEntity<Map> conLugar = anotar(cliente, encargado.restaurantId(), 2);
        assertThat(conLugar.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(conLugar.getBody()).containsEntry("mensaje", "Hay lugar en ese horario: reservá directamente");

        api.crearReserva(api.nuevoCliente(), encargado.restaurantId(), FECHA, 10);
        ResponseEntity<Map> anotado = anotar(cliente, encargado.restaurantId(), 2);
        assertThat(anotado.getBody()).containsEntry("estado", "ESPERANDO").containsEntry("horario", "20:00")
                .containsEntry("cantidad", 2);
        Long entrada = id(anotado);

        ResponseEntity<Map> repetido = anotar(cliente, encargado.restaurantId(), 3);
        assertThat(repetido.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(repetido.getBody()).containsEntry("mensaje", "Ya estás en la lista de espera de este restaurante ese día");

        ResponseEntity<List> mias = api.send(HttpMethod.GET, "/api/lista-espera/mias", cliente.token(), null, List.class);
        assertThat(mias.getBody()).hasSize(1);
        assertThat(((Map<?, ?>) mias.getBody().get(0)).get("id")).isEqualTo(entrada.intValue());
    }

    @Test
    void alLiberarseLugarSeAvisaALaPrimeraInscripcionQueEntra() {
        Encargado encargado = api.nuevoEncargado();
        Cliente ocupa = api.nuevoCliente();
        Long reserva = api.crearReserva(ocupa, encargado.restaurantId(), FECHA, 2);
        api.crearReserva(api.nuevoCliente(), encargado.restaurantId(), FECHA, 8);
        Cliente grande = api.nuevoCliente();
        Cliente chica = api.nuevoCliente();
        Long primera = id(anotar(grande, encargado.restaurantId(), 6));
        Long segunda = id(anotar(chica, encargado.restaurantId(), 2));

        api.send(HttpMethod.PUT, "/api/reservas/" + reserva, ocupa.token(), Map.of("estadoReserva", "CANCELADA"), Map.class);

        // Only 2 seats are free: the first entry (6 people) does not fit, the second one does.
        assertThat(estado(primera)).isEqualTo(EstadoListaEspera.ESPERANDO);
        assertThat(estado(segunda)).isEqualTo(EstadoListaEspera.NOTIFICADA);
        List<SimpleMailMessage> mails = mailsPara(chica.email());
        assertThat(mails).hasSize(1);
        assertThat(mails.get(0).getSubject()).startsWith("Foodies: se liberó un lugar en ");
        assertThat(mailsPara(grande.email())).isEmpty();

        // Booking that restaurant and day closes the client's entry.
        assertThat(api.reservar(chica, encargado.restaurantId(), FECHA, 2).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(estado(segunda)).isEqualTo(EstadoListaEspera.CANCELADA);
    }

    @Test
    void elClienteSaleDeLaListaYNoPuedeTocarLaDeOtro() {
        Encargado encargado = api.nuevoEncargado();
        api.crearReserva(api.nuevoCliente(), encargado.restaurantId(), FECHA, 10);
        Cliente cliente = api.nuevoCliente();
        Cliente otro = api.nuevoCliente();
        Long entrada = id(anotar(cliente, encargado.restaurantId(), 2));

        assertThat(api.status(HttpMethod.DELETE, "/api/lista-espera/" + entrada, otro.token(), null))
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(estado(entrada)).isEqualTo(EstadoListaEspera.ESPERANDO);
        assertThat(api.status(HttpMethod.DELETE, "/api/lista-espera/" + entrada, cliente.token(), null))
                .isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(estado(entrada)).isEqualTo(EstadoListaEspera.CANCELADA);
        assertThat(api.send(HttpMethod.GET, "/api/lista-espera/mias", cliente.token(), null, List.class).getBody()).isEmpty();
    }

    @Test
    void d6ConTresStrikesNoSePuedeAnotarYElEncargadoTampoco() {
        Encargado encargado = api.nuevoEncargado();
        api.crearReserva(api.nuevoCliente(), encargado.restaurantId(), FECHA, 10);
        Cliente cliente = api.nuevoCliente();
        Usuario usuario = usuarioRepo.findById(cliente.id()).orElseThrow();
        usuario.setStrikes(3);
        usuarioRepo.save(usuario);

        ResponseEntity<Map> resp = anotar(cliente, encargado.restaurantId(), 2);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(resp.getBody()).containsEntry("mensaje",
                "Tu cuenta tiene 3 strikes: no podés anotarte en listas de espera. Escribinos si creés que es un error.");
        assertThat(api.status(HttpMethod.GET, "/api/lista-espera/mias", encargado.token(), null))
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(listaRepo.findAllByUsuario_IdAndEstadoNotOrderByCreadaDesc(cliente.id(), EstadoListaEspera.CANCELADA))
                .isEmpty();
    }

    @Test
    void lasInscripcionesVencenCuandoPasaElHorario() {
        Encargado encargado = api.nuevoEncargado();
        api.crearReserva(api.nuevoCliente(), encargado.restaurantId(), FECHA, 10);
        Long entrada = id(anotar(api.nuevoCliente(), encargado.restaurantId(), 2));

        reloj.fijar(FECHA.atTime(19, 59));
        listaEsperaService.vencer();
        assertThat(estado(entrada)).isEqualTo(EstadoListaEspera.ESPERANDO);
        reloj.fijar(FECHA.atTime(20, 0));
        assertThat(listaEsperaService.vencer()).isPositive();

        assertThat(estado(entrada)).isEqualTo(EstadoListaEspera.VENCIDA);
    }
}
