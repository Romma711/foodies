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
import org.springframework.http.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** S5: bad input gets a 400 with a clear message, never a 500 or a stack trace. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RespuestasDeErrorTest {

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

    @Test
    void unIdQueNoEsNumeroResponde400ConMensaje() {
        ResponseEntity<Map> resp = rest.getForEntity("/api/restaurantes/abc", Map.class);

        assertBadRequestSinTrace(resp);
        assertThat((String) resp.getBody().get("mensaje")).contains("abc").contains("id");
    }

    @Test
    void unaEspecialidadInexistenteEnLaUrlResponde400ConLosValoresPosibles() {
        ResponseEntity<Map> resp = rest.getForEntity("/api/restaurantes/especialidad?especialidadDeComida=FOO", Map.class);

        assertBadRequestSinTrace(resp);
        assertThat((String) resp.getBody().get("mensaje")).contains("FOO").contains("PASTAS");
    }

    @Test
    void unParametroObligatorioFaltanteResponde400() {
        ResponseEntity<Map> resp = rest.getForEntity("/api/restaurantes/especialidad", Map.class);

        assertBadRequestSinTrace(resp);
        assertThat((String) resp.getBody().get("mensaje")).contains("especialidadDeComida");
    }

    @Test
    void unEnumInvalidoEnElCuerpoResponde400() {
        Encargado encargado = api.nuevoEncargado();
        Cliente cliente = api.nuevoCliente();
        Map<String, Object> body = new HashMap<>();
        body.put("cantidad", 2);
        body.put("estadoReserva", "FOO");
        body.put("fechaReserva", LocalDate.now().plusDays(1).toString());
        body.put("horarioLlegada", "20:00");
        body.put("idRestaurant", encargado.restaurantId());

        ResponseEntity<Map> resp = api.send(HttpMethod.POST, "/api/reservas", cliente.token(), body, Map.class);

        assertBadRequestSinTrace(resp);
        assertThat((String) resp.getBody().get("mensaje")).contains("FOO").contains("estadoReserva");
    }

    @Test
    void unJsonMalFormadoResponde400() {
        Cliente cliente = api.nuevoCliente();
        HttpHeaders headers = AutorizacionTestSupport.headers(cliente.token());
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<Map> resp = rest.exchange("/api/reservas", HttpMethod.POST,
                new HttpEntity<>("{\"cantidad\": ", headers), Map.class);

        assertBadRequestSinTrace(resp);
    }

    @Test
    void unaReservaSinCantidadResponde400() {
        Encargado encargado = api.nuevoEncargado();
        Cliente cliente = api.nuevoCliente();
        Map<String, Object> body = new HashMap<>();
        body.put("cantidad", null);
        body.put("fechaReserva", LocalDate.now().plusDays(1).toString());
        body.put("horarioLlegada", "20:00");
        body.put("idRestaurant", encargado.restaurantId());

        ResponseEntity<Map> resp = api.send(HttpMethod.POST, "/api/reservas", cliente.token(), body, Map.class);

        assertBadRequestSinTrace(resp);
        assertThat(resp.getBody()).containsEntry("mensaje", "La cantidad de personas debe ser mayor a 0");
    }

    @Test
    void unRegistroDeRestauranteConEspecialidadInexistenteResponde400() {
        Map<String, Object> datos = datosRestaurante();
        datos.put("especialidadDeComida", "SUSHI");

        ResponseEntity<Map> resp = rest.postForEntity("/api/auth/register/restaurante",
                RegistroRestauranteMultipart.conFotos(datos, 3), Map.class);

        assertBadRequestSinTrace(resp);
        assertThat(resp.getBody().toString()).contains("SUSHI");
    }

    @Test
    void unRegistroDeRestauranteConCupoNegativoResponde400() {
        Map<String, Object> datos = datosRestaurante();
        datos.put("cupoMaximo", -5);

        ResponseEntity<Map> resp = rest.postForEntity("/api/auth/register/restaurante",
                RegistroRestauranteMultipart.conFotos(datos, 3), Map.class);

        assertBadRequestSinTrace(resp);
        assertThat(resp.getBody()).containsKey("cupoMaximo");
    }

    @Test
    void unNombreDe5000CaracteresResponde400() {
        Map<String, Object> datos = datosRestaurante();
        datos.put("nombreRestaurante", "x".repeat(5000));
        ResponseEntity<Map> restaurante = rest.postForEntity("/api/auth/register/restaurante",
                RegistroRestauranteMultipart.conFotos(datos, 3), Map.class);
        assertBadRequestSinTrace(restaurante);

        ResponseEntity<Map> cliente = rest.postForEntity("/api/auth/register/cliente", Map.of(
                "nombre", "x".repeat(5000),
                "apellido", "Gómez",
                "email", "largo-" + UUID.randomUUID().toString().substring(0, 8) + "@test.com",
                "password", "secret1",
                "telefono", "2231111111"), Map.class);
        assertBadRequestSinTrace(cliente);
        assertThat(cliente.getBody()).containsKey("nombre");
    }

    @Test
    void losMensajesDe401Y403LleganEnUtf8() {
        Cliente cliente = api.nuevoCliente();

        ResponseEntity<byte[]> sinToken = rest.exchange("/api/reservas/usuario/1", HttpMethod.GET,
                new HttpEntity<>(new HttpHeaders()), byte[].class);
        assertThat(sinToken.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(new String(sinToken.getBody(), StandardCharsets.UTF_8)).contains("No estás autenticado");

        ResponseEntity<byte[]> sinPermiso = rest.exchange("/api/admin/requests", HttpMethod.GET,
                new HttpEntity<>(AutorizacionTestSupport.headers(cliente.token())), byte[].class);
        assertThat(sinPermiso.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(new String(sinPermiso.getBody(), StandardCharsets.UTF_8)).contains("No tenés permisos");
    }

    private static Map<String, Object> datosRestaurante() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Map<String, Object> datos = new HashMap<>();
        datos.put("email", "encargado-" + suffix + "@test.com");
        datos.put("password", "secret1");
        datos.put("nombreRestaurante", "Resto " + suffix);
        datos.put("direccion", "Calle " + suffix);
        datos.put("telefono", "2230000000");
        datos.put("especialidadDeComida", "PASTAS");
        datos.put("cupoMaximo", 10);
        return datos;
    }

    private static void assertBadRequestSinTrace(ResponseEntity<Map> resp) {
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody()).isNotNull().doesNotContainKey("trace");
        assertThat(resp.getBody().toString()).doesNotContain("Exception").doesNotContain("at com.");
    }
}
