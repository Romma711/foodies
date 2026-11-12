package com.example.Foodies;

import com.example.Foodies.CupoEspecial.CupoEspecial;
import com.example.Foodies.CupoEspecial.CupoEspecialRepository;
import com.example.Foodies.Enums.EspecialidadDeComida;
import com.example.Foodies.Enums.EstadoReserva;
import com.example.Foodies.Enums.Role;
import com.example.Foodies.Espacio.Espacio;
import com.example.Foodies.Espacio.EspacioRepository;
import com.example.Foodies.Espacio.EspaciosBackfill;
import com.example.Foodies.Espacio.TipoEspacio;
import com.example.Foodies.Reserva.Reserva;
import com.example.Foodies.Reserva.ReservaRepository;
import com.example.Foodies.Restaurant.Restaurant;
import com.example.Foodies.Restaurant.RestaurantRepository;
import com.example.Foodies.Usuario.Usuario;
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
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Espacios (salones, patios, barras) con capacidad propia por restaurante. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EspaciosTest {

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
    private EspacioRepository espacioRepo;
    @Autowired
    private ReservaRepository reservaRepo;
    @Autowired
    private CupoEspecialRepository cupoRepo;
    @Autowired
    private EspaciosBackfill backfill;

    private AutorizacionTestSupport api;

    @BeforeEach
    void setUp() {
        api = new AutorizacionTestSupport(rest, restaurantRepo);
    }

    // ---- B1: registro, backfill y cupoMaximo derivado ----

    @Test
    void elRegistroCreaElSalonPrincipalConLaCapacidadIndicada() {
        AutorizacionTestSupport.Encargado encargado = api.nuevoEncargado();

        List<Espacio> espacios = espacioRepo.findAllByRestaurant_IdOrderByIdAsc(encargado.restaurantId());
        assertThat(espacios).hasSize(1);
        Espacio salon = espacios.get(0);
        assertThat(salon.getNombre()).isEqualTo("Salón principal");
        assertThat(salon.getTipo()).isEqualTo(TipoEspacio.SALON);
        assertThat(salon.getCapacidad()).isEqualTo(10);
        assertThat(salon.getActivo()).isTrue();
        assertThat(restaurantRepo.findById(encargado.restaurantId()).orElseThrow().getCupoMaximo()).isEqualTo(10);
    }

    @Test
    void elBackfillCreaElSalonPrincipalYAsignaReservasYCuposSinEspacio() {
        Restaurant restaurant = restauranteSinEspacios(15);
        Reserva reserva = reservaSinEspacio(restaurant);
        CupoEspecial cupo = cupoSinEspacio(restaurant);

        backfill.run(null);
        backfill.run(null); // idempotente

        List<Espacio> espacios = espacioRepo.findAllByRestaurant_IdOrderByIdAsc(restaurant.getId());
        assertThat(espacios).hasSize(1);
        Espacio salon = espacios.get(0);
        assertThat(salon.getNombre()).isEqualTo("Salón principal");
        assertThat(salon.getTipo()).isEqualTo(TipoEspacio.SALON);
        assertThat(salon.getCapacidad()).isEqualTo(15);
        assertThat(reservaRepo.findById(reserva.getId()).orElseThrow().getEspacio().getId()).isEqualTo(salon.getId());
        assertThat(cupoRepo.findById(cupo.getId()).orElseThrow().getEspacio().getId()).isEqualTo(salon.getId());
    }

    @Test
    void elCupoMaximoYaNoSeEditaDesdeElPanelNiDesdeAdmin() {
        AutorizacionTestSupport.Encargado encargado = api.nuevoEncargado();

        ResponseEntity<Map> panel = api.send(HttpMethod.PATCH, "/api/restaurantes/me", encargado.token(),
                Map.of("cupoMaximo", 25), Map.class);
        assertThat(panel.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(panel.getBody().get("mensaje").toString()).contains("Espacios");

        ResponseEntity<Map> admin = api.send(HttpMethod.PATCH, "/api/restaurantes/" + encargado.restaurantId(),
                api.admin(), Map.of("cupoMaximo", 25), Map.class);
        assertThat(admin.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        assertThat(restaurantRepo.findById(encargado.restaurantId()).orElseThrow().getCupoMaximo()).isEqualTo(10);
    }

    // ---- B2: CRUD desde el panel y listado público ----

    @Test
    void elEncargadoAdministraSusEspaciosYLaCapacidadTotalSeRecalcula() {
        AutorizacionTestSupport.Encargado encargado = api.nuevoEncargado();
        String token = encargado.token();

        ResponseEntity<List> inicial = api.send(HttpMethod.GET, "/api/restaurantes/me/espacios", token, null, List.class);
        assertThat(inicial.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(inicial.getBody()).hasSize(1);

        ResponseEntity<Map> patio = api.send(HttpMethod.POST, "/api/restaurantes/me/espacios", token,
                Map.of("tipo", "PATIO", "nombre", "Patio trasero", "capacidad", 20), Map.class);
        assertThat(patio.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(patio.getBody()).containsEntry("tipo", "PATIO").containsEntry("nombre", "Patio trasero")
                .containsEntry("capacidad", 20).containsEntry("activo", true);
        Long patioId = ((Number) patio.getBody().get("id")).longValue();
        assertThat(cupoMaximo(encargado)).isEqualTo(30);

        ResponseEntity<Map> editado = api.send(HttpMethod.PUT, "/api/restaurantes/me/espacios/" + patioId, token,
                Map.of("tipo", "PATIO", "nombre", "Patio", "capacidad", 15), Map.class);
        assertThat(editado.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(editado.getBody()).containsEntry("nombre", "Patio").containsEntry("capacidad", 15);
        assertThat(cupoMaximo(encargado)).isEqualTo(25);

        ResponseEntity<Map> desactivado = api.send(HttpMethod.PATCH, "/api/restaurantes/me/espacios/" + patioId + "/activo",
                token, Map.of("activo", false), Map.class);
        assertThat(desactivado.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(desactivado.getBody()).containsEntry("activo", false);
        assertThat(cupoMaximo(encargado)).isEqualTo(10);

        api.send(HttpMethod.PATCH, "/api/restaurantes/me/espacios/" + patioId + "/activo", token, Map.of("activo", true), Map.class);
        assertThat(cupoMaximo(encargado)).isEqualTo(25);
    }

    @Test
    void losDatosDelEspacioSeValidan() {
        AutorizacionTestSupport.Encargado encargado = api.nuevoEncargado();
        String path = "/api/restaurantes/me/espacios";

        assertThat(mensaje(api.send(HttpMethod.POST, path, encargado.token(),
                Map.of("tipo", "SALON", "nombre", "salón PRINCIPAL", "capacidad", 5), Map.class)))
                .contains("Ya tenés un espacio con ese nombre");
        assertThat(mensaje(api.send(HttpMethod.POST, path, encargado.token(),
                Map.of("tipo", "BARRA", "nombre", "Barra", "capacidad", 0), Map.class)))
                .contains("capacidad");
        assertThat(mensaje(api.send(HttpMethod.POST, path, encargado.token(),
                Map.of("nombre", "Barra", "capacidad", 4), Map.class)))
                .contains("tipo");
        assertThat(mensaje(api.send(HttpMethod.POST, path, encargado.token(),
                Map.of("tipo", "BARRA", "nombre", " ", "capacidad", 4), Map.class)))
                .contains("nombre");
        assertThat(mensaje(api.send(HttpMethod.POST, path, encargado.token(),
                Map.of("tipo", "BARRA", "nombre", "x".repeat(61), "capacidad", 4), Map.class)))
                .contains("60");
        assertThat(mensaje(api.send(HttpMethod.POST, path, encargado.token(),
                Map.of("tipo", "TERRAZA", "nombre", "Terraza", "capacidad", 4), Map.class)))
                .isNotBlank();
        assertThat(espacioRepo.findAllByRestaurant_IdOrderByIdAsc(encargado.restaurantId())).hasSize(1);
    }

    @Test
    void noSePuedeDesactivarElUltimoEspacioActivo() {
        AutorizacionTestSupport.Encargado encargado = api.nuevoEncargado();
        Long salonId = salonPrincipal(encargado).getId();

        ResponseEntity<Map> resp = api.send(HttpMethod.PATCH, "/api/restaurantes/me/espacios/" + salonId + "/activo",
                encargado.token(), Map.of("activo", false), Map.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(mensaje(resp)).contains("al menos un espacio activo");
        assertThat(espacioRepo.findById(salonId).orElseThrow().getActivo()).isTrue();
    }

    @Test
    void losEspaciosSonSoloDeSuRestaurante() {
        AutorizacionTestSupport.Encargado encargado = api.nuevoEncargado();
        AutorizacionTestSupport.Encargado otro = api.nuevoEncargado();
        AutorizacionTestSupport.Cliente cliente = api.nuevoCliente();
        Long ajeno = salonPrincipal(otro).getId();

        assertThat(api.status(HttpMethod.PUT, "/api/restaurantes/me/espacios/" + ajeno, encargado.token(),
                Map.of("tipo", "SALON", "nombre", "Robado", "capacidad", 3))).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(api.status(HttpMethod.PATCH, "/api/restaurantes/me/espacios/" + ajeno + "/activo", encargado.token(),
                Map.of("activo", false))).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(api.status(HttpMethod.GET, "/api/restaurantes/me/espacios", null, null)).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(api.status(HttpMethod.GET, "/api/restaurantes/me/espacios", cliente.token(), null)).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(espacioRepo.findById(ajeno).orElseThrow().getNombre()).isEqualTo("Salón principal");
    }

    @Test
    void elListadoPublicoMuestraEspaciosActivosConLugarSinRevelarLaCapacidad() {
        AutorizacionTestSupport.Encargado encargado = api.nuevoEncargado();
        ResponseEntity<Map> barra = api.send(HttpMethod.POST, "/api/restaurantes/me/espacios", encargado.token(),
                Map.of("tipo", "BARRA", "nombre", "Barra", "capacidad", 4), Map.class);
        Long barraId = ((Number) barra.getBody().get("id")).longValue();
        api.send(HttpMethod.POST, "/api/restaurantes/me/espacios", encargado.token(),
                Map.of("tipo", "PATIO", "nombre", "Patio cerrado", "capacidad", 6), Map.class);
        Long patioId = espacioRepo.findAllByRestaurant_IdOrderByIdAsc(encargado.restaurantId()).get(2).getId();
        api.send(HttpMethod.PATCH, "/api/restaurantes/me/espacios/" + patioId + "/activo", encargado.token(),
                Map.of("activo", false), Map.class);
        String base = "/api/restaurantes/" + encargado.restaurantId() + "/espacios";

        ResponseEntity<List> lista = api.send(HttpMethod.GET, base, null, null, List.class);
        assertThat(lista.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(lista.getBody()).hasSize(2);
        @SuppressWarnings("unchecked")
        Map<String, Object> primero = (Map<String, Object>) lista.getBody().get(0);
        assertThat(primero).containsEntry("nombre", "Salón principal").containsEntry("tipo", "SALON")
                .doesNotContainKey("capacidad").containsEntry("hayLugar", null);

        String fecha = LocalDate.now().plusDays(2).toString();
        ResponseEntity<List> conLugar = api.send(HttpMethod.GET, base + "?fecha=" + fecha + "&cantidad=5", null, null, List.class);
        assertThat(conLugar.getBody()).extracting(e -> ((Map<?, ?>) e).get("hayLugar")).containsExactly(true, false);
        assertThat(((Map<?, ?>) conLugar.getBody().get(1)).get("id")).isEqualTo(barraId.intValue());

        assertThat(api.status(HttpMethod.GET, base + "?fecha=" + fecha, null, null)).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(api.status(HttpMethod.GET, "/api/restaurantes/999999/espacios", null, null)).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ---- B3: reservas por espacio ----

    @Test
    void laReservaExigeUnEspacioActivoDelRestaurante() {
        AutorizacionTestSupport.Encargado encargado = api.nuevoEncargado();
        AutorizacionTestSupport.Encargado otro = api.nuevoEncargado();
        AutorizacionTestSupport.Cliente cliente = api.nuevoCliente();
        LocalDate fecha = LocalDate.now().plusDays(2);
        Long patioId = crearEspacio(encargado, "PATIO", "Patio", 4);
        Long salonId = salonPrincipal(encargado).getId();

        assertThat(mensaje(api.reservar(cliente, encargado.restaurantId(), null, fecha, 2)))
                .contains("Elegí en qué espacio");
        assertThat(mensaje(api.reservar(cliente, encargado.restaurantId(), salonPrincipal(otro).getId(), fecha, 2)))
                .contains("no está disponible");
        api.send(HttpMethod.PATCH, "/api/restaurantes/me/espacios/" + patioId + "/activo", encargado.token(),
                Map.of("activo", false), Map.class);
        assertThat(mensaje(api.reservar(cliente, encargado.restaurantId(), patioId, fecha, 2)))
                .contains("no está disponible");

        ResponseEntity<Map> ok = api.reservar(cliente, encargado.restaurantId(), salonId, fecha, 2);
        assertThat(ok.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(ok.getBody()).containsEntry("espacioNombre", "Salón principal")
                .containsEntry("idEspacio", salonId.intValue());
    }

    @Test
    void elCupoSeValidaPorEspacio() {
        AutorizacionTestSupport.Encargado encargado = api.nuevoEncargado();
        AutorizacionTestSupport.Cliente cliente = api.nuevoCliente();
        LocalDate fecha = LocalDate.now().plusDays(3);
        Long salonId = salonPrincipal(encargado).getId();
        Long patioId = crearEspacio(encargado, "PATIO", "Patio", 4);

        assertThat(api.reservar(cliente, encargado.restaurantId(), salonId, fecha, 10).getStatusCode()).isEqualTo(HttpStatus.OK);
        ResponseEntity<Map> salonLleno = api.reservar(cliente, encargado.restaurantId(), salonId, fecha, 1);
        assertThat(salonLleno.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(salonLleno.getBody().toString()).doesNotContain("10");

        assertThat(api.reservar(cliente, encargado.restaurantId(), patioId, fecha, 4).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(api.reservar(cliente, encargado.restaurantId(), patioId, fecha, 1).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        ResponseEntity<List> publico = api.send(HttpMethod.GET, "/api/restaurantes/" + encargado.restaurantId()
                + "/espacios?fecha=" + fecha + "&cantidad=1", null, null, List.class);
        assertThat(publico.getBody()).extracting(e -> ((Map<?, ?>) e).get("hayLugar")).containsExactly(false, false);
    }

    @Test
    void laAutoAceptacionSeCalculaSobreElCupoDeCadaEspacio() {
        AutorizacionTestSupport.Encargado encargado = api.nuevoEncargado();
        AutorizacionTestSupport.Cliente cliente = api.nuevoCliente();
        LocalDate fecha = LocalDate.now().plusDays(4);
        Long salonId = salonPrincipal(encargado).getId();
        Long patioId = crearEspacio(encargado, "PATIO", "Patio", 4);
        api.send(HttpMethod.PATCH, "/api/restaurantes/me", encargado.token(), Map.of("porcentajeAutoAceptacion", 50), Map.class);

        assertThat(estado(api.reservar(cliente, encargado.restaurantId(), patioId, fecha, 2))).isEqualTo("ACEPTADA");
        assertThat(estado(api.reservar(cliente, encargado.restaurantId(), patioId, fecha, 1))).isEqualTo("PENDIENTE");
        assertThat(estado(api.reservar(cliente, encargado.restaurantId(), salonId, fecha, 5))).isEqualTo("ACEPTADA");
    }

    @Test
    void alModificarSePuedeCambiarDeEspacioYSeValidaSuCupo() {
        AutorizacionTestSupport.Encargado encargado = api.nuevoEncargado();
        AutorizacionTestSupport.Cliente cliente = api.nuevoCliente();
        LocalDate fecha = LocalDate.now().plusDays(5);
        Long salonId = salonPrincipal(encargado).getId();
        Long barraId = crearEspacio(encargado, "BARRA", "Barra", 3);
        Long reservaId = ((Number) api.reservar(cliente, encargado.restaurantId(), salonId, fecha, 4)
                .getBody().get("id")).longValue();
        String path = "/api/reservas/" + reservaId;

        ResponseEntity<Map> sinLugar = api.send(HttpMethod.PUT, path, cliente.token(), Map.of("idEspacio", barraId), Map.class);
        assertThat(sinLugar.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(reservaRepo.findById(reservaId).orElseThrow().getEspacio().getId()).isEqualTo(salonId);

        ResponseEntity<Map> movida = api.send(HttpMethod.PUT, path, cliente.token(),
                Map.of("idEspacio", barraId, "cantidad", 3), Map.class);
        assertThat(movida.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(movida.getBody()).containsEntry("espacioNombre", "Barra").containsEntry("estadoReserva", "PENDIENTE");
        assertThat(reservaRepo.findById(reservaId).orElseThrow().getEspacio().getId()).isEqualTo(barraId);
    }

    @Test
    void noSePuedeDesactivarUnEspacioConReservasFuturas() {
        AutorizacionTestSupport.Encargado encargado = api.nuevoEncargado();
        AutorizacionTestSupport.Cliente cliente = api.nuevoCliente();
        Long patioId = crearEspacio(encargado, "PATIO", "Patio", 4);
        api.reservar(cliente, encargado.restaurantId(), patioId, LocalDate.now().plusDays(6), 2);

        ResponseEntity<Map> resp = api.send(HttpMethod.PATCH, "/api/restaurantes/me/espacios/" + patioId + "/activo",
                encargado.token(), Map.of("activo", false), Map.class);
        assertThat(mensaje(resp)).contains("reservas");
        assertThat(espacioRepo.findById(patioId).orElseThrow().getActivo()).isTrue();

        ResponseEntity<Map> achicar = api.send(HttpMethod.PUT, "/api/restaurantes/me/espacios/" + patioId,
                encargado.token(), Map.of("tipo", "PATIO", "nombre", "Patio", "capacidad", 1), Map.class);
        assertThat(mensaje(achicar)).contains("No podés bajar la capacidad");
    }

    @Test
    void elPanelYMisReservasMuestranElEspacio() {
        AutorizacionTestSupport.Encargado encargado = api.nuevoEncargado();
        AutorizacionTestSupport.Cliente cliente = api.nuevoCliente();
        Long patioId = crearEspacio(encargado, "PATIO", "Patio", 4);
        api.reservar(cliente, encargado.restaurantId(), patioId, LocalDate.now().plusDays(2), 2);

        ResponseEntity<List> panel = api.send(HttpMethod.GET, "/api/restaurantes/me/reservas", encargado.token(), null, List.class);
        assertThat(panel.getBody()).extracting(r -> ((Map<?, ?>) r).get("espacioNombre")).containsExactly("Patio");

        ResponseEntity<List> mias = api.send(HttpMethod.GET, "/api/reservas/usuario/" + cliente.id(), cliente.token(), null, List.class);
        assertThat(mias.getBody()).extracting(r -> ((Map<?, ?>) r).get("espacioNombre")).containsExactly("Patio");
        assertThat(mias.getBody()).extracting(r -> ((Map<?, ?>) r).get("idEspacio")).containsExactly(patioId.intValue());
        assertThat(mias.getBody()).extracting(r -> ((Map<?, ?>) r).get("idRestaurant"))
                .containsExactly(encargado.restaurantId().intValue());
    }

    // ---- B4: cupos especiales por espacio y fecha ----

    @Test
    void elCupoEspecialAplicaSoloAlEspacioIndicado() {
        AutorizacionTestSupport.Encargado encargado = api.nuevoEncargado();
        AutorizacionTestSupport.Cliente cliente = api.nuevoCliente();
        LocalDate fecha = LocalDate.now().plusDays(7);
        Long salonId = salonPrincipal(encargado).getId();
        Long patioId = crearEspacio(encargado, "PATIO", "Patio", 4);

        ResponseEntity<Map> patioCerrado = api.send(HttpMethod.PUT, cupoPath(patioId, fecha), encargado.token(),
                Map.of("cupo", 0, "motivo", "Lluvia"), Map.class);
        assertThat(patioCerrado.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(patioCerrado.getBody()).containsEntry("espacioNombre", "Patio")
                .containsEntry("idEspacio", patioId.intValue()).containsEntry("cupo", 0);
        // El mismo día el salón también puede tener su propio cupo especial.
        assertThat(api.send(HttpMethod.PUT, cupoPath(salonId, fecha), encargado.token(), Map.of("cupo", 6), Map.class)
                .getStatusCode()).isEqualTo(HttpStatus.OK);

        assertThat(api.reservar(cliente, encargado.restaurantId(), patioId, fecha, 1).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(api.reservar(cliente, encargado.restaurantId(), salonId, fecha, 6).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(api.reservar(cliente, encargado.restaurantId(), salonId, fecha, 1).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        // Otro día el patio vuelve a su capacidad normal.
        assertThat(api.reservar(cliente, encargado.restaurantId(), patioId, fecha.plusDays(1), 4).getStatusCode())
                .isEqualTo(HttpStatus.OK);

        ResponseEntity<List> lista = api.send(HttpMethod.GET, "/api/restaurantes/me/cupos-especiales",
                encargado.token(), null, List.class);
        assertThat(lista.getBody()).extracting(c -> ((Map<?, ?>) c).get("espacioNombre"))
                .containsExactlyInAnyOrder("Patio", "Salón principal");
        assertThat(lista.getBody()).extracting(c -> ((Map<?, ?>) c).get("reservadas"))
                .containsExactlyInAnyOrder(0, 6);

        assertThat(api.status(HttpMethod.DELETE, cupoPath(patioId, fecha), encargado.token(), null))
                .isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(api.reservar(cliente, encargado.restaurantId(), patioId, fecha, 4).getStatusCode())
                .isEqualTo(HttpStatus.OK);
    }

    @Test
    void elCupoEspecialNoSuperaLaCapacidadDelEspacioYEsSoloDelRestaurante() {
        AutorizacionTestSupport.Encargado encargado = api.nuevoEncargado();
        AutorizacionTestSupport.Encargado otro = api.nuevoEncargado();
        LocalDate fecha = LocalDate.now().plusDays(8);
        Long patioId = crearEspacio(encargado, "PATIO", "Patio", 4);

        ResponseEntity<Map> excede = api.send(HttpMethod.PUT, cupoPath(patioId, fecha), encargado.token(),
                Map.of("cupo", 5), Map.class);
        assertThat(mensaje(excede)).contains("(4)");

        assertThat(api.status(HttpMethod.PUT, cupoPath(salonPrincipal(otro).getId(), fecha), encargado.token(),
                Map.of("cupo", 1))).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(cupoRepo.findAll()).noneMatch(c -> c.getEspacio() != null
                && c.getEspacio().getId().equals(salonPrincipal(otro).getId()));
    }

    // ---- helpers ----

    private static String cupoPath(Long espacioId, LocalDate fecha) {
        return "/api/restaurantes/me/espacios/" + espacioId + "/cupos-especiales/" + fecha;
    }

    private Long crearEspacio(AutorizacionTestSupport.Encargado encargado, String tipo, String nombre, int capacidad) {
        ResponseEntity<Map> resp = api.send(HttpMethod.POST, "/api/restaurantes/me/espacios", encargado.token(),
                Map.of("tipo", tipo, "nombre", nombre, "capacidad", capacidad), Map.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return ((Number) resp.getBody().get("id")).longValue();
    }

    private static String estado(ResponseEntity<Map> resp) {
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        return resp.getBody().get("estadoReserva").toString();
    }

    private int cupoMaximo(AutorizacionTestSupport.Encargado encargado) {
        return restaurantRepo.findById(encargado.restaurantId()).orElseThrow().getCupoMaximo();
    }

    private Espacio salonPrincipal(AutorizacionTestSupport.Encargado encargado) {
        return espacioRepo.findAllByRestaurant_IdOrderByIdAsc(encargado.restaurantId()).get(0);
    }

    private static String mensaje(ResponseEntity<Map> resp) {
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Object mensaje = resp.getBody() == null ? null : resp.getBody().get("mensaje");
        return mensaje == null ? String.valueOf(resp.getBody()) : mensaje.toString();
    }

    private Restaurant restauranteSinEspacios(int cupoMaximo) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Usuario usuario = new Usuario();
        usuario.setNombre("Encargado");
        usuario.setApellido(suffix);
        usuario.setEmail("legacy-" + suffix + "@test.com");
        usuario.setPassword("secret");
        usuario.setTelefono("2230000000");
        usuario.setRol(Role.ROLE_ENCARGADO);
        Restaurant restaurant = new Restaurant();
        restaurant.setNombre("Legacy " + suffix);
        restaurant.setUbicacion("Calle " + suffix);
        restaurant.setCupoMaximo(cupoMaximo);
        restaurant.setAprobado(true);
        restaurant.setEspecialidad(EspecialidadDeComida.PASTAS);
        restaurant.setUsuario(usuario);
        return restaurantRepo.saveAndFlush(restaurant);
    }

    private Reserva reservaSinEspacio(Restaurant restaurant) {
        Reserva reserva = new Reserva();
        reserva.setCantidad(2);
        reserva.setFechaReserva(Date.from(LocalDate.now().plusDays(3).atStartOfDay(ZoneId.systemDefault()).toInstant()));
        reserva.setHorariollegada(LocalTime.of(20, 0));
        reserva.setEstadoReserva(EstadoReserva.PENDIENTE);
        reserva.setRestaurant(restaurant);
        return reservaRepo.saveAndFlush(reserva);
    }

    private CupoEspecial cupoSinEspacio(Restaurant restaurant) {
        CupoEspecial cupo = new CupoEspecial();
        cupo.setRestaurant(restaurant);
        cupo.setFecha(LocalDate.now().plusDays(4));
        cupo.setCupo(5);
        return cupoRepo.saveAndFlush(cupo);
    }
}
