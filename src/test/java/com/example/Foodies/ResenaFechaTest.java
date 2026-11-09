package com.example.Foodies;

import com.example.Foodies.AutorizacionTestSupport.Cliente;
import com.example.Foodies.AutorizacionTestSupport.Encargado;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** S6: reviews store when they were written and the API returns them newest first. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ResenaFechaTest {

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
    private JdbcTemplate jdbc;

    private AutorizacionTestSupport api;

    @BeforeEach
    void setUp() {
        api = new AutorizacionTestSupport(rest, restaurantRepo);
    }

    @Test
    void unaResenaNuevaGuardaYDevuelveSuFechaDeCreacion() {
        Encargado encargado = api.nuevoEncargado();
        Cliente cliente = api.nuevoCliente();
        LocalDateTime antes = LocalDateTime.now().minusSeconds(1);

        ResponseEntity<Map> creada = resenar(cliente, encargado.restaurantId());

        assertThat(creada.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        LocalDateTime fecha = LocalDateTime.parse((String) creada.getBody().get("fechaCreacion"));
        assertThat(fecha).isAfter(antes).isBefore(LocalDateTime.now().plusSeconds(1));

        ResponseEntity<List> lista = rest.getForEntity("/api/resenas?id=" + encargado.restaurantId(), List.class);
        assertThat(((Map<?, ?>) lista.getBody().get(0)).get("fechaCreacion")).isNotNull();
        ResponseEntity<List> delUsuario = api.send(HttpMethod.GET, "/api/resenas/usuario/" + cliente.id(),
                cliente.token(), null, List.class);
        assertThat(((Map<?, ?>) delUsuario.getBody().get(0)).get("fechaCreacion")).isNotNull();
    }

    @Test
    void lasResenasDeUnRestauranteVienenDeLaMasNuevaALaMasViejaYLasSinFechaAlFinal() {
        Encargado encargado = api.nuevoEncargado();
        long primera = id(resenar(api.nuevoCliente(), encargado.restaurantId()));
        long segunda = id(resenar(api.nuevoCliente(), encargado.restaurantId()));
        long sinFecha = id(resenar(api.nuevoCliente(), encargado.restaurantId()));
        // The first review is the most recent one by date even though its id is the smallest.
        jdbc.update("update resena set fecha_creacion = ? where id = ?", LocalDateTime.now().plusMinutes(5), primera);
        jdbc.update("update resena set fecha_creacion = null where id = ?", sinFecha);

        ResponseEntity<List> lista = rest.getForEntity("/api/resenas?id=" + encargado.restaurantId(), List.class);

        assertThat(lista.getBody()).extracting(r -> ((Number) ((Map<?, ?>) r).get("id")).longValue())
                .containsExactly(primera, segunda, sinFecha);
    }

    private ResponseEntity<Map> resenar(Cliente cliente, Long restaurantId) {
        return api.send(HttpMethod.POST, "/api/resenas", cliente.token(), Map.of(
                "comentario", "Muy rica la comida, volvería.",
                "calificacion", 5,
                "restaurantId", restaurantId), Map.class);
    }

    private static long id(ResponseEntity<Map> resp) {
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return ((Number) resp.getBody().get("id")).longValue();
    }
}
