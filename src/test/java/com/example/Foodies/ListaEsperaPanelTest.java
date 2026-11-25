package com.example.Foodies;

import com.example.Foodies.AutorizacionTestSupport.Cliente;
import com.example.Foodies.AutorizacionTestSupport.Encargado;
import com.example.Foodies.ListaEspera.EntradaListaEspera;
import com.example.Foodies.ListaEspera.EstadoListaEspera;
import com.example.Foodies.ListaEspera.ListaEsperaRepository;
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
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** S7d (D23): the restaurant panel shows its own open waitlist entries, read only. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(RelojDePrueba.Config.class)
class ListaEsperaPanelTest {

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
    private RelojDePrueba reloj;
    @MockitoBean
    private JavaMailSender mailSender;

    private AutorizacionTestSupport api;
    private static final LocalDate HOY = LocalDate.now();
    private static final LocalDate FECHA = HOY.plusDays(2);

    @BeforeEach
    void setUp() {
        reloj.reiniciar();
        api = new AutorizacionTestSupport(rest, restaurantRepo);
    }

    @AfterEach
    void tearDown() {
        reloj.reiniciar();
    }

    private Long anotar(Cliente cliente, Long restaurantId, LocalDate fecha, String horario, int cantidad, LocalDateTime cuando) {
        reloj.fijar(cuando);
        Map<String, Object> body = new HashMap<>();
        body.put("idRestaurant", restaurantId);
        body.put("fechaReserva", fecha.toString());
        body.put("horarioLlegada", horario);
        body.put("cantidad", cantidad);
        ResponseEntity<Map> resp = api.send(HttpMethod.POST, "/api/lista-espera", cliente.token(), body, Map.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        reloj.reiniciar();
        return ((Number) resp.getBody().get("id")).longValue();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> panel(Encargado encargado, String fecha) {
        String path = "/api/restaurantes/me/lista-espera" + (fecha == null ? "" : "?fecha=" + fecha);
        ResponseEntity<List> resp = api.send(HttpMethod.GET, path, encargado.token(), null, List.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        return resp.getBody();
    }

    private static String nombre(Cliente cliente) {
        return "Cliente " + cliente.email().substring("cliente-".length(), "cliente-".length() + 8);
    }

    @Test
    void elRestauranteVeSoloSusInscripcionesAbiertasOrdenadas() {
        Encargado encargado = api.nuevoEncargado();
        Encargado otro = api.nuevoEncargado();
        api.crearReserva(api.nuevoCliente(), encargado.restaurantId(), FECHA, 10);
        api.crearReserva(api.nuevoCliente(), encargado.restaurantId(), FECHA.plusDays(1), 10);
        api.crearReserva(api.nuevoCliente(), otro.restaurantId(), FECHA, 10);
        Cliente ana = api.nuevoCliente();
        Cliente beto = api.nuevoCliente();
        Cliente caro = api.nuevoCliente();
        Cliente dani = api.nuevoCliente();
        Cliente eli = api.nuevoCliente();

        Long tarde = anotar(ana, encargado.restaurantId(), FECHA, "21:00", 2, HOY.atTime(9, 0, 5));
        Long temprano = anotar(beto, encargado.restaurantId(), FECHA, "20:00", 3, HOY.atTime(9, 10));
        Long notificada = anotar(caro, encargado.restaurantId(), FECHA, "20:00", 1, HOY.atTime(9, 20));
        Long otroDia = anotar(dani, encargado.restaurantId(), FECHA.plusDays(1), "20:00", 4, HOY.atTime(8, 0));
        Long salio = anotar(eli, encargado.restaurantId(), FECHA, "20:00", 2, HOY.atTime(8, 30));
        Long ajena = anotar(eli, otro.restaurantId(), FECHA, "20:00", 2, HOY.atTime(8, 30));
        assertThat(api.status(HttpMethod.DELETE, "/api/lista-espera/" + salio, eli.token(), null)).isEqualTo(HttpStatus.NO_CONTENT);
        EntradaListaEspera aviso = listaRepo.findById(notificada).orElseThrow();
        aviso.setEstado(EstadoListaEspera.NOTIFICADA);
        listaRepo.save(aviso);

        List<Map<String, Object>> lista = panel(encargado, null);

        assertThat(lista).extracting(e -> ((Number) e.get("id")).longValue())
                .containsExactly(temprano, notificada, tarde, otroDia);
        assertThat(lista.get(0)).containsOnlyKeys("id", "fecha", "horario", "idTurno", "cantidad", "estado", "creada", "cliente")
                .containsAllEntriesOf(Map.of(
                        "id", temprano.intValue(),
                        "fecha", FECHA.toString(),
                        "horario", "20:00",
                        "cantidad", 3,
                        "estado", "ESPERANDO",
                        "creada", HOY + "T09:10:00",
                        "cliente", nombre(beto)));
        assertThat(lista.get(1)).containsEntry("estado", "NOTIFICADA");
        assertThat(lista.get(2)).containsEntry("creada", HOY + "T09:00:05").containsEntry("cliente", nombre(ana));
        assertThat(lista).allSatisfy(e -> assertThat(e).containsEntry("idTurno", null));
        assertThat(lista).extracting(e -> ((Number) e.get("id")).longValue()).doesNotContain(salio, ajena);

        // Read only: nothing changed.
        assertThat(listaRepo.findById(temprano).orElseThrow().getEstado()).isEqualTo(EstadoListaEspera.ESPERANDO);
        assertThat(listaRepo.findById(notificada).orElseThrow().getEstado()).isEqualTo(EstadoListaEspera.NOTIFICADA);
    }

    @Test
    void conFechaMuestraSoloEseDiaYSinFechaNoMuestraElPasado() {
        Encargado encargado = api.nuevoEncargado();
        api.crearReserva(api.nuevoCliente(), encargado.restaurantId(), FECHA, 10);
        api.crearReserva(api.nuevoCliente(), encargado.restaurantId(), FECHA.plusDays(1), 10);
        Long delDia = anotar(api.nuevoCliente(), encargado.restaurantId(), FECHA, "20:00", 2, HOY.atTime(9, 0));
        Long siguiente = anotar(api.nuevoCliente(), encargado.restaurantId(), FECHA.plusDays(1), "20:00", 2, HOY.atTime(9, 0));
        Long pasada = anotar(api.nuevoCliente(), encargado.restaurantId(), FECHA, "21:00", 2, HOY.atTime(9, 0));
        EntradaListaEspera ayer = listaRepo.findById(pasada).orElseThrow();
        ayer.setFecha(HOY.minusDays(1));
        listaRepo.save(ayer);

        assertThat(panel(encargado, FECHA.toString())).extracting(e -> ((Number) e.get("id")).longValue())
                .containsExactly(delDia);
        assertThat(panel(encargado, FECHA.plusDays(1).toString())).extracting(e -> ((Number) e.get("id")).longValue())
                .containsExactly(siguiente);
        assertThat(panel(encargado, null)).extracting(e -> ((Number) e.get("id")).longValue())
                .containsExactly(delDia, siguiente);
        assertThat(panel(encargado, HOY.minusDays(1).toString())).extracting(e -> ((Number) e.get("id")).longValue())
                .containsExactly(pasada);
        assertThat(panel(encargado, HOY.plusDays(20).toString())).isEmpty();
    }

    @Test
    void sinNombreMuestraElEmail() {
        Encargado encargado = api.nuevoEncargado();
        api.crearReserva(api.nuevoCliente(), encargado.restaurantId(), FECHA, 10);
        Cliente cliente = api.nuevoCliente();
        anotar(cliente, encargado.restaurantId(), FECHA, "20:00", 2, HOY.atTime(9, 0));
        Usuario usuario = usuarioRepo.findById(cliente.id()).orElseThrow();
        usuario.setNombre(" ");
        usuario.setApellido(null);
        usuarioRepo.save(usuario);

        assertThat(panel(encargado, null).get(0)).containsEntry("cliente", cliente.email());
    }

    @Test
    void unaFechaInvalidaResponde400ConCodigo() {
        Encargado encargado = api.nuevoEncargado();
        for (String fecha : new String[]{"hola", "2026-13-01", "25/12/2026"}) {
            ResponseEntity<Map> resp = api.send(HttpMethod.GET, "/api/restaurantes/me/lista-espera?fecha=" + fecha,
                    encargado.token(), null, Map.class);
            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(resp.getBody()).containsEntry("codigo", "FECHA_FORMATO_INVALIDO");
        }
    }

    @Test
    void soloElEncargadoVeLaListaDeEspera() {
        Cliente cliente = api.nuevoCliente();
        assertThat(api.status(HttpMethod.GET, "/api/restaurantes/me/lista-espera", cliente.token(), null))
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(api.status(HttpMethod.GET, "/api/restaurantes/me/lista-espera", api.admin(), null))
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(api.status(HttpMethod.GET, "/api/restaurantes/me/lista-espera", null, null))
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
