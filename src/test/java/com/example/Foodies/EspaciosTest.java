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

    // ---- helpers ----

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
