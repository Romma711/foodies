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
import java.util.Arrays;
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
        ResponseEntity<Map> detalle = get("/api/restaurantes/" + encargado.restaurantId(), null);
        assertThat(detalle.getStatusCode()).isEqualTo(HttpStatus.OK);
        // La capacidad es un dato interno del restaurante: el catálogo público no la expone.
        assertThat(detalle.getBody()).containsEntry("nombre", encargado.nombre()).doesNotContainKey("cupoMaximo");
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

        ResponseEntity<Map> sinLugar = reservar(cliente, encargado.restaurantId(), fecha, 1);
        assertThat(sinLugar.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        // El cliente se entera de que no hay lugar, pero no de cuál es la capacidad.
        assertThat(sinLugar.getBody()).containsEntry("mensaje",
                "No hay lugar para 1 persona ese día. Probá con otra fecha o menos personas.");

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

    @Test
    void elEncargadoGestionaLosCuposEspecialesDeSuRestaurante() {
        Encargado encargado = nuevoEncargado();
        Encargado otro = nuevoEncargado();
        Cliente cliente = nuevoCliente();
        LocalDate fecha = LocalDate.now().plusDays(8);
        String path = "/api/restaurantes/me/cupos-especiales/" + fecha;

        assertThat(send(HttpMethod.PUT, path, null, Map.of("cupo", 4)).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(send(HttpMethod.PUT, path, cliente.token(), Map.of("cupo", 4)).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);

        ResponseEntity<Map> creado = send(HttpMethod.PUT, path, encargado.token(),
                Map.of("cupo", 4, "motivo", "Cumpleaños privado"));
        assertThat(creado.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(creado.getBody())
                .containsEntry("fecha", fecha.toString())
                .containsEntry("cupo", 4)
                .containsEntry("motivo", "Cumpleaños privado")
                .containsEntry("reservadas", 0);

        // Volver a guardar la misma fecha la modifica, no crea otra.
        send(HttpMethod.PUT, path, encargado.token(), Map.of("cupo", 3));
        ResponseEntity<List> lista = listarCuposEspeciales(encargado.token());
        assertThat(lista.getBody()).hasSize(1);
        assertThat(((Map<?, ?>) lista.getBody().get(0)).get("cupo")).isEqualTo(3);
        assertThat(listarCuposEspeciales(otro.token()).getBody()).isEmpty();

        assertThat(send(HttpMethod.DELETE, path, encargado.token(), null).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(listarCuposEspeciales(encargado.token()).getBody()).isEmpty();
    }

    @Test
    void elCupoEspecialSeValida() {
        Encargado encargado = nuevoEncargado();
        String base = "/api/restaurantes/me/cupos-especiales/";
        LocalDate fecha = LocalDate.now().plusDays(9);

        assertThat(send(HttpMethod.PUT, base + fecha, encargado.token(), Map.of("cupo", -1)).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        // Sirve para reducir la capacidad: no puede superar la capacidad normal (10).
        assertThat(send(HttpMethod.PUT, base + fecha, encargado.token(), Map.of("cupo", 11)).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(send(HttpMethod.PUT, base + LocalDate.now().minusDays(1), encargado.token(), Map.of("cupo", 4))
                .getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(listarCuposEspeciales(encargado.token()).getBody()).isEmpty();
    }

    @Test
    void noSePuedeBajarElCupoPorDebajoDeLoYaReservado() {
        Encargado encargado = nuevoEncargado();
        Cliente cliente = nuevoCliente();
        LocalDate fecha = LocalDate.now().plusDays(10);
        String path = "/api/restaurantes/me/cupos-especiales/" + fecha;
        assertThat(reservar(cliente, encargado.restaurantId(), fecha, 6).getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<Map> rechazado = send(HttpMethod.PUT, path, encargado.token(), Map.of("cupo", 5));
        assertThat(rechazado.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(rechazado.getBody()).containsEntry("mensaje",
                "Ya hay 6 personas reservadas ese día. No podés bajar el cupo a menos de 6.");
        assertThat(listarCuposEspeciales(encargado.token()).getBody()).isEmpty();

        ResponseEntity<Map> aceptado = send(HttpMethod.PUT, path, encargado.token(), Map.of("cupo", 6));
        assertThat(aceptado.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(aceptado.getBody()).containsEntry("reservadas", 6);
    }

    @Test
    void lasReservasDeEseDiaUsanElCupoEspecial() {
        Encargado encargado = nuevoEncargado();
        Cliente cliente = nuevoCliente();
        LocalDate fecha = LocalDate.now().plusDays(11);
        send(HttpMethod.PUT, "/api/restaurantes/me/cupos-especiales/" + fecha, encargado.token(), Map.of("cupo", 4));

        assertThat(reservar(cliente, encargado.restaurantId(), fecha, 4).getStatusCode()).isEqualTo(HttpStatus.OK);
        ResponseEntity<Map> sinLugar = reservar(cliente, encargado.restaurantId(), fecha, 1);
        assertThat(sinLugar.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat((String) sinLugar.getBody().get("mensaje")).startsWith("No hay lugar");

        // Los demás días siguen con la capacidad normal.
        assertThat(reservar(cliente, encargado.restaurantId(), fecha.plusDays(1), 10).getStatusCode())
                .isEqualTo(HttpStatus.OK);
    }

    @Test
    void laAceptacionAutomaticaSeCalculaSobreElCupoEspecial() {
        Encargado encargado = nuevoEncargado();
        Cliente cliente = nuevoCliente();
        LocalDate fecha = LocalDate.now().plusDays(12);
        // 50% de un cupo especial de 4: se aceptan solas hasta 2 personas.
        send(HttpMethod.PATCH, "/api/restaurantes/me", encargado.token(), Map.of("porcentajeAutoAceptacion", 50));
        send(HttpMethod.PUT, "/api/restaurantes/me/cupos-especiales/" + fecha, encargado.token(), Map.of("cupo", 4));

        assertThat(estado(reservar(cliente, encargado.restaurantId(), fecha, 2))).isEqualTo("ACEPTADA");
        assertThat(estado(reservar(cliente, encargado.restaurantId(), fecha, 1))).isEqualTo("PENDIENTE");
    }

    @Test
    void elRegistroExigeEntreTresYCincoFotosConUnaDelFrente() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String nombre = "Resto fotos " + suffix;
        Map<String, Object> datos = datosRegistro("fotos-" + suffix + "@test.com", nombre, suffix);
        String url = "/api/auth/register/restaurante";

        // Sin fotos, con pocas, con demasiadas, sin la del frente, con un archivo que no es imagen o muy pesado:
        // se rechaza y no se crea nada.
        var sinFotos = RegistroRestauranteMultipart.entity(RegistroRestauranteMultipart.soloDatos(datos));
        var sinFrente = RegistroRestauranteMultipart.soloDatos(datos);
        for (int i = 0; i < 3; i++) {
            sinFrente.add("salones", RegistroRestauranteMultipart.archivo("s" + i + ".jpg", RegistroRestauranteMultipart.JPEG));
        }
        var noEsImagen = RegistroRestauranteMultipart.soloDatos(datos);
        noEsImagen.add("frente", RegistroRestauranteMultipart.archivo("frente.jpg", "no soy una foto".getBytes(StandardCharsets.UTF_8)));
        noEsImagen.add("salones", RegistroRestauranteMultipart.archivo("s1.jpg", RegistroRestauranteMultipart.JPEG));
        noEsImagen.add("salones", RegistroRestauranteMultipart.archivo("s2.jpg", RegistroRestauranteMultipart.JPEG));
        var muyGrande = RegistroRestauranteMultipart.soloDatos(datos);
        byte[] grande = Arrays.copyOf(RegistroRestauranteMultipart.JPEG, 5 * 1024 * 1024 + 1);
        muyGrande.add("frente", RegistroRestauranteMultipart.archivo("frente.jpg", grande));
        muyGrande.add("salones", RegistroRestauranteMultipart.archivo("s1.jpg", RegistroRestauranteMultipart.JPEG));
        muyGrande.add("salones", RegistroRestauranteMultipart.archivo("s2.jpg", RegistroRestauranteMultipart.JPEG));

        List<HttpEntity<?>> invalidos = List.of(
                sinFotos,
                RegistroRestauranteMultipart.conFotos(datos, 1),
                RegistroRestauranteMultipart.conFotos(datos, 5),
                RegistroRestauranteMultipart.entity(sinFrente),
                RegistroRestauranteMultipart.entity(noEsImagen),
                RegistroRestauranteMultipart.entity(muyGrande));
        for (HttpEntity<?> invalido : invalidos) {
            assertThat(rest.postForEntity(url, invalido, Map.class).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
        assertThat(restaurantRepo.findAll()).noneMatch(r -> r.getNombre().equals(nombre));

        // 1 del frente + 4 del salón = 5: el máximo permitido.
        assertThat(rest.postForEntity(url, RegistroRestauranteMultipart.conFotos(datos, 4), Map.class).getStatusCode())
                .isEqualTo(HttpStatus.CREATED);
        Long id = restaurantRepo.findAll().stream().filter(r -> r.getNombre().equals(nombre))
                .map(Restaurant::getId).findFirst().orElseThrow();
        assertThat(rest.exchange("/api/restaurantes/" + id + "/fotos", HttpMethod.GET, null, List.class).getBody())
                .hasSize(5);
    }

    @Test
    void laFotoDelFrenteEsLaPortadaDelCatalogo() {
        Encargado encargado = nuevoEncargado();

        ResponseEntity<List> fotos = rest.exchange("/api/restaurantes/" + encargado.restaurantId() + "/fotos",
                HttpMethod.GET, null, List.class);
        assertThat(fotos.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(fotos.getBody()).hasSize(3);
        Map<?, ?> frente = (Map<?, ?>) fotos.getBody().get(0);
        assertThat(frente.get("tipo")).isEqualTo("FRENTE");
        assertThat(((Map<?, ?>) fotos.getBody().get(1)).get("tipo")).isEqualTo("SALON");

        ResponseEntity<List> catalogo = rest.exchange("/api/restaurantes", HttpMethod.GET, null, List.class);
        Map<?, ?> item = (Map<?, ?>) catalogo.getBody().stream()
                .filter(r -> ((Number) ((Map<?, ?>) r).get("id")).longValue() == encargado.restaurantId())
                .findFirst().orElseThrow();
        assertThat(item.get("fotoPortada")).isEqualTo(frente.get("url"));

        ResponseEntity<byte[]> imagen = rest.getForEntity((String) frente.get("url"), byte[].class);
        assertThat(imagen.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(imagen.getHeaders().getContentType()).isEqualTo(MediaType.IMAGE_JPEG);
    }

    @Test
    void elEncargadoGestionaSusFotosRespetandoLosLimites() {
        Encargado encargado = nuevoEncargado();
        Encargado otro = nuevoEncargado();
        List<Map<?, ?>> fotos = misFotos(encargado.token());
        assertThat(fotos).hasSize(3);
        Long frenteId = idDe(fotos.get(0));
        Long salonId = idDe(fotos.get(1));

        assertThat(subirFoto(encargado.token(), "SALON", RegistroRestauranteMultipart.WEBP).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(subirFoto(encargado.token(), "SALON", RegistroRestauranteMultipart.JPEG).getStatusCode()).isEqualTo(HttpStatus.OK);
        ResponseEntity<Map> sexta = subirFoto(encargado.token(), "SALON", RegistroRestauranteMultipart.JPEG);
        assertThat(sexta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(sexta.getBody()).containsEntry("mensaje", "Podés tener hasta 5 fotos. Borrá una antes de subir otra.");
        assertThat(subirFoto(encargado.token(), "FRENTE", RegistroRestauranteMultipart.JPEG).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        // La del frente no se borra: se reemplaza, y el archivo anterior se elimina.
        assertThat(send(HttpMethod.DELETE, "/api/restaurantes/me/fotos/" + frenteId, encargado.token(), null)
                .getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Path frenteViejo = storedFile((String) fotos.get(0).get("url"));
        ResponseEntity<Map> reemplazada = reemplazarFoto(encargado.token(), frenteId, RegistroRestauranteMultipart.PNG);
        assertThat(reemplazada.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(reemplazada.getBody()).containsEntry("tipo", "FRENTE");
        assertThat(frenteViejo).doesNotExist();
        assertThat(storedFile((String) reemplazada.getBody().get("url"))).exists();

        // Otro encargado no puede tocar estas fotos.
        assertThat(send(HttpMethod.DELETE, "/api/restaurantes/me/fotos/" + salonId, otro.token(), null)
                .getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        // Se puede bajar hasta 2 fotos del salón, no menos.
        List<Long> salones = misFotos(encargado.token()).stream()
                .filter(f -> "SALON".equals(f.get("tipo"))).map(RestaurantePanelTest::idDe).toList();
        assertThat(salones).hasSize(4);
        assertThat(send(HttpMethod.DELETE, "/api/restaurantes/me/fotos/" + salones.get(0), encargado.token(), null)
                .getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(send(HttpMethod.DELETE, "/api/restaurantes/me/fotos/" + salones.get(1), encargado.token(), null)
                .getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        ResponseEntity<Map> minimo = send(HttpMethod.DELETE, "/api/restaurantes/me/fotos/" + salones.get(2), encargado.token(), null);
        assertThat(minimo.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(minimo.getBody()).containsEntry("mensaje",
                "Tenés que tener al menos 2 fotos del salón. Subí otra antes de borrar esta.");
        assertThat(misFotos(encargado.token())).hasSize(3);
    }

    @Test
    void elAdminVeLasFotosDeLosRestaurantesAAprobar() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String nombre = "Resto pendiente " + suffix;
        assertThat(rest.postForEntity("/api/auth/register/restaurante",
                RegistroRestauranteMultipart.conFotos(datosRegistro("pend-" + suffix + "@test.com", nombre, suffix), 2),
                Map.class).getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<List> pendientes = rest.exchange("/api/admin/requests", HttpMethod.GET,
                new HttpEntity<>(headers(login("admin@foodies.com", "admin123"))), List.class);
        Map<?, ?> pendiente = (Map<?, ?>) pendientes.getBody().stream()
                .filter(p -> nombre.equals(((Map<?, ?>) p).get("nombre"))).findFirst().orElseThrow();
        assertThat((List<?>) pendiente.get("fotos")).hasSize(3);
    }

    // --- helpers ---

    private List<Map<?, ?>> misFotos(String token) {
        ResponseEntity<List> resp = rest.exchange("/api/restaurantes/me/fotos", HttpMethod.GET,
                new HttpEntity<>(headers(token)), List.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        return ((List<?>) resp.getBody()).stream().<Map<?, ?>>map(f -> (Map<?, ?>) f).toList();
    }

    private static Long idDe(Map<?, ?> foto) {
        return ((Number) foto.get("id")).longValue();
    }

    private ResponseEntity<Map> subirFoto(String token, String tipo, byte[] contenido) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("tipo", tipo);
        body.add("archivo", RegistroRestauranteMultipart.archivo("foto.img", contenido));
        HttpHeaders headers = headers(token);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        return rest.exchange("/api/restaurantes/me/fotos", HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);
    }

    private ResponseEntity<Map> reemplazarFoto(String token, Long fotoId, byte[] contenido) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("archivo", RegistroRestauranteMultipart.archivo("foto.img", contenido));
        HttpHeaders headers = headers(token);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        return rest.exchange("/api/restaurantes/me/fotos/" + fotoId, HttpMethod.PUT, new HttpEntity<>(body, headers), Map.class);
    }

    private ResponseEntity<List> listarCuposEspeciales(String token) {
        return rest.exchange("/api/restaurantes/me/cupos-especiales", HttpMethod.GET,
                new HttpEntity<>(headers(token)), List.class);
    }

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
        ResponseEntity<UsuarioDetailDTO> reg = rest.postForEntity("/api/auth/register/restaurante",
                RegistroRestauranteMultipart.conFotos(datosRegistro(email, nombre, suffix), 2), UsuarioDetailDTO.class);
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

    private static Map<String, Object> datosRegistro(String email, String nombre, String suffix) {
        return Map.of(
                "email", email,
                "password", "secret1",
                "nombreRestaurante", nombre,
                "direccion", "Calle " + suffix,
                "telefono", "2230000000",
                "especialidadDeComida", "PASTAS",
                "cupoMaximo", 10);
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
