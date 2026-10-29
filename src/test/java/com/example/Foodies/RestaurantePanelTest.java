package com.example.Foodies;

import com.example.Foodies.Restaurant.Restaurant;
import com.example.Foodies.Restaurant.RestaurantRepository;
import com.example.Foodies.Usuario.dtos.UsuarioDetailDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RestaurantePanelTest {

    private static final byte[] PDF = "%PDF-1.4\n%test menu\n".getBytes(StandardCharsets.US_ASCII);

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

    private record Encargado(String token, Long restaurantId, String nombre) {}

    private record Cliente(String token, Long id) {}

    @Test
    void meRequiresAnEncargadoAndReturnsTheirOwnRestaurant() {
        Encargado encargado = nuevoEncargado();
        Cliente cliente = nuevoCliente();

        assertThat(get("/api/restaurantes/me", null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(get("/api/restaurantes/me", cliente.token()).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        ResponseEntity<Map> me = get("/api/restaurantes/me", encargado.token());
        assertThat(me.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(me.getBody()).containsEntry("nombre", encargado.nombre());
        assertThat(((Number) me.getBody().get("id")).longValue()).isEqualTo(encargado.restaurantId());
        assertThat(me.getBody().get("carta")).isNull();
    }

    @Test
    void patchMeUpdatesTheOwnRestaurantAndValidatesInput() {
        Encargado encargado = nuevoEncargado();

        ResponseEntity<Map> updated = send(HttpMethod.PATCH, "/api/restaurantes/me", encargado.token(),
                Map.of("nombre", "Nuevo nombre", "cupoMaximo", 25));
        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updated.getBody()).containsEntry("nombre", "Nuevo nombre").containsEntry("cupoMaximo", 25);
        assertThat(restaurantRepo.findById(encargado.restaurantId()).orElseThrow().getCupoMaximo()).isEqualTo(25);

        ResponseEntity<Map> invalid = send(HttpMethod.PATCH, "/api/restaurantes/me", encargado.token(),
                Map.of("cupoMaximo", 0));
        assertThat(invalid.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(restaurantRepo.findById(encargado.restaurantId()).orElseThrow().getCupoMaximo()).isEqualTo(25);
    }

    @Test
    void writesByIdAreNoLongerPublicNorOpenToOtherManagers() {
        Encargado encargado = nuevoEncargado();
        Encargado otro = nuevoEncargado();
        String path = "/api/restaurantes/" + otro.restaurantId();

        assertThat(send(HttpMethod.DELETE, path, null, null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(send(HttpMethod.PATCH, path, null, Map.of("nombre", "x")).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(send(HttpMethod.PATCH, path, encargado.token(), Map.of("nombre", "x")).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(send(HttpMethod.DELETE, "/api/carta/1", null, null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(restaurantRepo.findById(otro.restaurantId()).orElseThrow().getNombre()).isEqualTo(otro.nombre());
    }

    @Test
    void publicCatalogReadsStillWork() {
        Encargado encargado = nuevoEncargado();

        assertThat(rest.getForEntity("/api/restaurantes", String.class).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(get("/api/restaurantes/" + encargado.restaurantId(), null).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void cartaIsStoredOutsideTheDatabaseAndCanBeReplacedAndDeleted() throws Exception {
        Encargado encargado = nuevoEncargado();

        ResponseEntity<Map> uploaded = uploadCarta(encargado.token(), "menu.pdf", PDF);
        assertThat(uploaded.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(uploaded.getBody()).containsEntry("nombreArchivo", "menu.pdf");
        String firstUrl = (String) uploaded.getBody().get("url");
        Path firstFile = storedFile(firstUrl);
        assertThat(firstFile).exists();

        ResponseEntity<Map> publicCarta = get("/api/restaurantes/" + encargado.restaurantId() + "/carta", null);
        assertThat(publicCarta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(publicCarta.getBody()).containsEntry("url", firstUrl).containsEntry("nombreArchivo", "menu.pdf");

        ResponseEntity<byte[]> download = rest.getForEntity(firstUrl, byte[].class);
        assertThat(download.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(download.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        assertThat(download.getBody()).isEqualTo(PDF);
        // The panel previews the PDF in an iframe served from the same origin.
        assertThat(download.getHeaders().getFirst("X-Frame-Options")).isEqualTo("SAMEORIGIN");
        assertThat(rest.getForEntity("/api/files", String.class).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        ResponseEntity<Map> replaced = uploadCarta(encargado.token(), "menu-2.pdf", PDF);
        assertThat(replaced.getStatusCode()).isEqualTo(HttpStatus.OK);
        Path secondFile = storedFile((String) replaced.getBody().get("url"));
        assertThat(secondFile).exists();
        assertThat(firstFile).doesNotExist();

        assertThat(send(HttpMethod.DELETE, "/api/restaurantes/me/carta", encargado.token(), null).getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(secondFile).doesNotExist();
        assertThat(get("/api/restaurantes/" + encargado.restaurantId() + "/carta", null).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void cartaRejectsFilesThatAreNotPdf() throws Exception {
        Encargado encargado = nuevoEncargado();

        ResponseEntity<Map> response = uploadCarta(encargado.token(), "menu.pdf", "not a pdf".getBytes(StandardCharsets.UTF_8));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        try (var files = Files.walk(storageDir)) {
            assertThat(files.filter(Files::isRegularFile)
                    .filter(f -> f.toString().contains("cartas" + java.io.File.separator + encargado.restaurantId())))
                    .isEmpty();
        }
    }

    @Test
    void managerSeesTheirReservationsAndOnlyChangesTheirOwn() {
        Encargado encargado = nuevoEncargado();
        Encargado otro = nuevoEncargado();
        Cliente cliente = nuevoCliente();
        LocalDate fecha = LocalDate.now().plusDays(3);
        Long reservaId = crearReserva(cliente, encargado.restaurantId(), fecha);

        ResponseEntity<List> vacias = rest.exchange("/api/restaurantes/me/reservas", HttpMethod.GET,
                new HttpEntity<>(headers(otro.token())), List.class);
        assertThat(vacias.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(vacias.getBody()).isEmpty();

        ResponseEntity<List> reservas = rest.exchange("/api/restaurantes/me/reservas", HttpMethod.GET,
                new HttpEntity<>(headers(encargado.token())), List.class);
        assertThat(reservas.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(reservas.getBody()).hasSize(1);
        Map<?, ?> reserva = (Map<?, ?>) reservas.getBody().get(0);
        assertThat(reserva.get("fecha")).isEqualTo(fecha.toString());
        assertThat(reserva.get("estado")).isEqualTo("PENDIENTE");
        assertThat((String) reserva.get("nombreCliente")).startsWith("Cliente");

        String estadoPath = "/api/reservas/" + reservaId + "/estado";
        assertThat(send(HttpMethod.PATCH, estadoPath, otro.token(), Map.of("estado", "CANCELADA")).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(send(HttpMethod.PATCH, estadoPath, encargado.token(), Map.of("estado", "PENDIENTE")).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        ResponseEntity<Map> aceptada = send(HttpMethod.PATCH, estadoPath, encargado.token(), Map.of("estado", "ACEPTADA"));
        assertThat(aceptada.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(aceptada.getBody()).containsEntry("estado", "ACEPTADA");
    }

    @Test
    void elCupoSeCalculaPorRestauranteYSinContarLasCanceladas() {
        Encargado encargado = nuevoEncargado();
        Encargado otro = nuevoEncargado();
        Cliente cliente = nuevoCliente();
        LocalDate fecha = LocalDate.now().plusDays(5);

        // El otro restaurante llena su cupo (10) ese mismo día: no debe afectar al primero.
        assertThat(reservar(cliente, otro.restaurantId(), fecha, 10).getStatusCode()).isEqualTo(HttpStatus.OK);
        ResponseEntity<Map> llena = reservar(cliente, encargado.restaurantId(), fecha, 10);
        assertThat(llena.getStatusCode()).isEqualTo(HttpStatus.OK);

        assertThat(reservar(cliente, encargado.restaurantId(), fecha, 1).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        // Al cancelarla, el cupo vuelve a estar libre.
        String estadoPath = "/api/reservas/" + ((Number) llena.getBody().get("id")).longValue() + "/estado";
        assertThat(send(HttpMethod.PATCH, estadoPath, encargado.token(), Map.of("estado", "CANCELADA")).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(reservar(cliente, encargado.restaurantId(), fecha, 10).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void elEncargadoConfiguraElPorcentajeDeAutoAceptacion() {
        Encargado encargado = nuevoEncargado();

        assertThat(get("/api/restaurantes/me", encargado.token()).getBody())
                .containsEntry("porcentajeAutoAceptacion", 0);

        ResponseEntity<Map> actualizado = send(HttpMethod.PATCH, "/api/restaurantes/me", encargado.token(),
                Map.of("porcentajeAutoAceptacion", 60));
        assertThat(actualizado.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(actualizado.getBody()).containsEntry("porcentajeAutoAceptacion", 60);
        assertThat(restaurantRepo.findById(encargado.restaurantId()).orElseThrow().getPorcentajeAutoAceptacion())
                .isEqualTo(60);

        for (int invalido : new int[]{-1, 101}) {
            ResponseEntity<Map> rechazado = send(HttpMethod.PATCH, "/api/restaurantes/me", encargado.token(),
                    Map.of("porcentajeAutoAceptacion", invalido));
            assertThat(rechazado.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
        assertThat(restaurantRepo.findById(encargado.restaurantId()).orElseThrow().getPorcentajeAutoAceptacion())
                .isEqualTo(60);

        // Cambiar otro campo no apaga la auto-aceptación.
        send(HttpMethod.PATCH, "/api/restaurantes/me", encargado.token(), Map.of("nombre", "Otro nombre"));
        assertThat(restaurantRepo.findById(encargado.restaurantId()).orElseThrow().getPorcentajeAutoAceptacion())
                .isEqualTo(60);

        ResponseEntity<Map> apagado = send(HttpMethod.PATCH, "/api/restaurantes/me", encargado.token(),
                Map.of("porcentajeAutoAceptacion", 0));
        assertThat(apagado.getBody()).containsEntry("porcentajeAutoAceptacion", 0);
    }

    @Test
    void lasReservasSeAceptanSolasHastaElPorcentajeYLaQueLoCruzaQuedaPendiente() {
        Encargado encargado = nuevoEncargado();
        Cliente cliente = nuevoCliente();
        LocalDate fecha = LocalDate.now().plusDays(6);
        // Cupo 10 al 60%: se aceptan solas hasta 6 personas.
        send(HttpMethod.PATCH, "/api/restaurantes/me", encargado.token(), Map.of("porcentajeAutoAceptacion", 60));

        assertThat(estado(reservar(cliente, encargado.restaurantId(), fecha, 2))).isEqualTo("ACEPTADA");
        assertThat(estado(reservar(cliente, encargado.restaurantId(), fecha, 2))).isEqualTo("ACEPTADA");
        // 4 aceptadas + 3 = 7 > 6: cruza el límite, queda pendiente.
        assertThat(estado(reservar(cliente, encargado.restaurantId(), fecha, 3))).isEqualTo("PENDIENTE");
        // 4 aceptadas + 2 = 6: justo en el límite, se acepta.
        assertThat(estado(reservar(cliente, encargado.restaurantId(), fecha, 2))).isEqualTo("ACEPTADA");
        // Ya hay 6 aceptadas: todo lo que sigue queda pendiente.
        assertThat(estado(reservar(cliente, encargado.restaurantId(), fecha, 1))).isEqualTo("PENDIENTE");
    }

    @Test
    void sinAutoAceptacionLasReservasQuedanPendientes() {
        Encargado encargado = nuevoEncargado();
        Cliente cliente = nuevoCliente();

        assertThat(estado(reservar(cliente, encargado.restaurantId(), LocalDate.now().plusDays(7), 1)))
                .isEqualTo("PENDIENTE");
    }

    // --- helpers ---

    private ResponseEntity<Map> reservar(Cliente cliente, Long restaurantId, LocalDate fecha, int cantidad) {
        return send(HttpMethod.POST, "/api/reservas", cliente.token(), Map.of(
                "cantidad", cantidad,
                "fechaReserva", fecha.toString(),
                "horarioLlegada", "20:00",
                "idUsuario", cliente.id(),
                "idRestaurant", restaurantId));
    }

    private static String estado(ResponseEntity<Map> reserva) {
        assertThat(reserva.getStatusCode()).isEqualTo(HttpStatus.OK);
        return (String) reserva.getBody().get("estadoReserva");
    }

    private ResponseEntity<Map> get(String path, String token) {
        return rest.exchange(path, HttpMethod.GET, new HttpEntity<>(headers(token)), Map.class);
    }

    private ResponseEntity<Map> send(HttpMethod method, String path, String token, Object body) {
        HttpHeaders headers = headers(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return rest.exchange(path, method, new HttpEntity<>(body, headers), Map.class);
    }

    private ResponseEntity<Map> uploadCarta(String token, String filename, byte[] content) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("archivo", new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return filename;
            }
        });
        HttpHeaders headers = headers(token);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        return rest.exchange("/api/restaurantes/me/carta", HttpMethod.PUT, new HttpEntity<>(body, headers), Map.class);
    }

    /** Local storage URLs look like `/api/files/<key>`; the key maps to a path under the storage dir. */
    private Path storedFile(String url) {
        assertThat(url).startsWith("/api/files/");
        return storageDir.resolve(url.substring("/api/files/".length()));
    }

    private HttpHeaders headers(String token) {
        HttpHeaders headers = new HttpHeaders();
        if (token != null) {
            headers.setBearerAuth(token);
        }
        return headers;
    }

    private String login(String email, String password) {
        ResponseEntity<Map> resp = rest.postForEntity("/api/auth/login", Map.of("email", email, "password", password), Map.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        return ((String) resp.getBody().get("Token")).substring("Bearer ".length());
    }

    private Encargado nuevoEncargado() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String email = "encargado-" + suffix + "@test.com";
        String nombre = "Resto " + suffix;
        ResponseEntity<UsuarioDetailDTO> reg = rest.postForEntity("/api/auth/register/restaurante", Map.of(
                "email", email,
                "password", "secret1",
                "nombreRestaurante", nombre,
                "direccion", "Calle " + suffix,
                "telefono", "2230000000",
                "especialidadDeComida", "PASTAS",
                "cupoMaximo", 10), UsuarioDetailDTO.class);
        assertThat(reg.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<UsuarioDetailDTO> aprobado = rest.exchange("/api/admin/approved/{id}", HttpMethod.PUT,
                new HttpEntity<>(headers(login("admin@foodies.com", "admin123"))), UsuarioDetailDTO.class,
                reg.getBody().id());
        assertThat(aprobado.getStatusCode()).isEqualTo(HttpStatus.OK);

        Long restaurantId = restaurantRepo.findAll().stream()
                .filter(r -> r.getNombre().equals(nombre))
                .map(Restaurant::getId)
                .findFirst()
                .orElseThrow();
        return new Encargado(login(email, "secret1"), restaurantId, nombre);
    }

    private Cliente nuevoCliente() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String email = "cliente-" + suffix + "@test.com";
        ResponseEntity<UsuarioDetailDTO> reg = rest.postForEntity("/api/auth/register/cliente", Map.of(
                "nombre", "Cliente",
                "apellido", suffix,
                "email", email,
                "password", "secret1",
                "telefono", "2231111111"), UsuarioDetailDTO.class);
        assertThat(reg.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return new Cliente(login(email, "secret1"), reg.getBody().id());
    }

    private Long crearReserva(Cliente cliente, Long restaurantId, LocalDate fecha) {
        ResponseEntity<Map> resp = send(HttpMethod.POST, "/api/reservas", cliente.token(), Map.of(
                "cantidad", 2,
                "fechaReserva", fecha.toString(),
                "horarioLlegada", "20:00",
                "idUsuario", cliente.id(),
                "idRestaurant", restaurantId));
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        return ((Number) resp.getBody().get("id")).longValue();
    }
}
