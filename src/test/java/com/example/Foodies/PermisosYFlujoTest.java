package com.example.Foodies;

import com.example.Foodies.Restaurant.Restaurant;
import com.example.Foodies.Restaurant.RestaurantRepository;
import com.example.Foodies.Usuario.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests de regresión de los permisos y reglas de negocio del flujo:
 * IDOR en clientes, PATCH que no pisa campos, reseñas siempre propias,
 * restaurantes pendientes ocultos, login unificado, borrados con historial,
 * carta solo PDF, cupo por restaurante y encargado solo ve lo suyo.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PermisosYFlujoTest extends TestBase {

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private RestaurantRepository restaurantRepo;

    @Autowired
    private UsuarioRepository usuarioRepo;

    // ---------- helpers ----------

    private String login(String email, String password) {
        ResponseEntity<Map> resp = rest.postForEntity("/api/auth/login",
                new HttpEntity<>(Map.of("email", email, "password", password)), Map.class);
        if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null
                && resp.getBody().get("Token") != null) {
            return resp.getBody().get("Token").toString().substring("Bearer ".length());
        }
        return null;
    }

    private String adminToken() {
        return login("admin@foodies.com", "admin123");
    }

    private HttpHeaders auth(String token, boolean json) {
        HttpHeaders h = new HttpHeaders();
        if (token != null) h.setBearerAuth(token);
        if (json) h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }

    private Long registrarCliente(String email, String nombre) {
        ResponseEntity<Map> resp = rest.postForEntity("/api/auth/register/cliente",
                new HttpEntity<>(Map.of(
                        "nombre", nombre, "apellido", "Apellido",
                        "email", email, "password", "secret1", "telefono", "111")),
                Map.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return Long.valueOf(resp.getBody().get("id").toString());
    }

    private Long idResto(String nombre) {
        return restaurantRepo.findAll().stream()
                .filter(r -> r.getNombre().equals(nombre))
                .map(Restaurant::getId)
                .findFirst()
                .orElseThrow();
    }

    private Long registrarResto(String email, String nombre, int cupo, String especialidad, boolean aprobar) {
        ResponseEntity<Map> reg = rest.postForEntity("/api/auth/register/restaurante",
                new HttpEntity<>(Map.of(
                        "email", email, "password", "secret1",
                        "nombreRestaurante", nombre, "direccion", "Calle 10",
                        "telefono", "222", "especialidadDeComida", especialidad, "cupoMaximo", cupo)),
                Map.class);
        assertThat(reg.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Long encargadoId = Long.valueOf(reg.getBody().get("id").toString());
        if (aprobar) {
            ResponseEntity<String> ok = rest.exchange("/api/admin/approved/{id}", HttpMethod.PUT,
                    new HttpEntity<>(auth(adminToken(), false)), String.class, encargadoId);
            assertThat(ok.getStatusCode()).isEqualTo(HttpStatus.OK);
        }
        return idResto(nombre);
    }

    private String fecha(int dias) {
        return LocalDate.now().plusDays(dias).format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    private ResponseEntity<String> crearReserva(String token, long usuarioId, long restoId,
                                                int cantidad, String dia, String hora) {
        return rest.exchange("/api/reservas", HttpMethod.POST,
                new HttpEntity<>(Map.of(
                        "cantidad", cantidad, "fechaReserva", dia, "horarioLlegada", hora,
                        "idUsuario", usuarioId, "idRestaurant", restoId),
                        auth(token, true)),
                String.class);
    }

    // ---------- tests ----------

    @Test
    void loginFallidoResponde401ConMensajeGenerico() {
        ResponseEntity<String> sinUsuario = rest.postForEntity("/api/auth/login",
                new HttpEntity<>(Map.of("email", "noexiste@test.com", "password", "x")), String.class);
        ResponseEntity<String> passwordMala = rest.postForEntity("/api/auth/login",
                new HttpEntity<>(Map.of("email", "admin@foodies.com", "password", "mala")), String.class);

        assertThat(sinUsuario.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(passwordMala.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(sinUsuario.getBody()).contains("Email o contraseña incorrectos");
        assertThat(passwordMala.getBody()).contains("Email o contraseña incorrectos");
    }

    @Test
    void clientesNoSePuedenVerNiModificarEntreSi() {
        String emailPropio = "reg-idor@test.com";
        if (login(emailPropio, "secret1") == null) {
            registrarCliente(emailPropio, "Alice");
        }
        String token = login(emailPropio, "secret1");
        Long idPropio = usuarioRepo.findByEmail(emailPropio).orElseThrow().getId();
        Long idAjeno = registrarCliente("reg-ajeno@test.com", "Carla");

        // listado: solo admin
        ResponseEntity<String> listaComoCliente = rest.exchange("/api/clientes", HttpMethod.GET,
                new HttpEntity<>(auth(token, false)), String.class);
        assertThat(listaComoCliente.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        ResponseEntity<String> listaComoAdmin = rest.exchange("/api/clientes", HttpMethod.GET,
                new HttpEntity<>(auth(adminToken(), false)), String.class);
        assertThat(listaComoAdmin.getStatusCode()).isEqualTo(HttpStatus.OK);

        // lectura: propio 200 / ajeno 403
        ResponseEntity<String> ajeno = rest.exchange("/api/clientes/{id}", HttpMethod.GET,
                new HttpEntity<>(auth(token, false)), String.class, idAjeno);
        assertThat(ajeno.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        ResponseEntity<String> propio = rest.exchange("/api/clientes/{id}", HttpMethod.GET,
                new HttpEntity<>(auth(token, false)), String.class, idPropio);
        assertThat(propio.getStatusCode()).isEqualTo(HttpStatus.OK);

        // patch: ajeno 403
        ResponseEntity<String> patchAjeno = rest.exchange("/api/clientes/{id}", HttpMethod.PATCH,
                new HttpEntity<>(Map.of("telefono", "999"), auth(token, true)), String.class, idAjeno);
        assertThat(patchAjeno.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        // patch propio: solo actualiza lo enviado y no pisa el resto
        ResponseEntity<String> patchPropio = rest.exchange("/api/clientes/{id}", HttpMethod.PATCH,
                new HttpEntity<>(Map.of("telefono", "555"), auth(token, true)), String.class, idPropio);
        assertThat(patchPropio.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(patchPropio.getBody()).contains("\"telefono\":\"555\"");
        assertThat(patchPropio.getBody()).contains("\"nombre\":\"Alice\"");

        // borrado ajeno
        ResponseEntity<String> borrarAjeno = rest.exchange("/api/clientes/{id}", HttpMethod.DELETE,
                new HttpEntity<>(auth(token, false)), String.class, idAjeno);
        assertThat(borrarAjeno.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void borradoDeClientesBloqueadoPorHistorial() {
        String admin = adminToken();
        Long restoId = registrarResto("reg-hist-resto@test.com", "RestoHistorial", 10, "PARRILLA", true);

        // cliente con reserva -> 409
        Long conReserva = registrarCliente("reg-conreserva@test.com", "Diana");
        String token = login("reg-conreserva@test.com", "secret1");
        ResponseEntity<String> reserva = crearReserva(token, conReserva, restoId, 1, fecha(30), "19:30");
        assertThat(reserva.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<String> borrarConHistorial = rest.exchange("/api/clientes/{id}", HttpMethod.DELETE,
                new HttpEntity<>(auth(admin, false)), String.class, conReserva);
        assertThat(borrarConHistorial.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(borrarConHistorial.getBody()).contains("reservas o reseñas asociadas");

        // cliente sin historial -> 204
        Long sinHistorial = registrarCliente("reg-sinhistorial@test.com", "Ema");
        ResponseEntity<String> borrarLimpio = rest.exchange("/api/clientes/{id}", HttpMethod.DELETE,
                new HttpEntity<>(auth(admin, false)), String.class, sinHistorial);
        assertThat(borrarLimpio.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void resenasPertenecenAlUsuarioDelToken() {
        String admin = adminToken();
        Long restoId = registrarResto("reg-resena-resto@test.com", "RestoResena", 10, "PESCADOS", true);

        Long idA = registrarCliente("reg-resena-a@test.com", "Alice");
        String tokenA = login("reg-resena-a@test.com", "secret1");
        Long idB = registrarCliente("reg-resena-b@test.com", "Bob");
        String tokenB = login("reg-resena-b@test.com", "secret1");

        // el usuarioId del body se ignora: el autor es el del token
        ResponseEntity<String> creada = rest.exchange("/api/resenas", HttpMethod.POST,
                new HttpEntity<>(Map.of("comentario", "muy bueno", "calificacion", 5,
                        "usuarioId", idB, "restaurantId", restoId), auth(tokenA, true)),
                String.class);
        assertThat(creada.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(creada.getBody()).contains("\"nombreUsuario\":\"Alice\"");
        Long resenaId = Long.valueOf(creada.getBody().replaceAll(".*\"id\":(\\d+).*", "$1"));

        // el admin no reseña
        ResponseEntity<String> creadaPorAdmin = rest.exchange("/api/resenas", HttpMethod.POST,
                new HttpEntity<>(Map.of("comentario", "de admin", "calificacion", 5,
                        "restaurantId", restoId), auth(admin, true)),
                String.class);
        assertThat(creadaPorAdmin.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        // editar: ajena 403, propia 200
        ResponseEntity<String> putAjena = rest.exchange("/api/resenas/{id}", HttpMethod.PUT,
                new HttpEntity<>(Map.of("calificacion", 1), auth(tokenB, true)), String.class, resenaId);
        assertThat(putAjena.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        ResponseEntity<String> putPropia = rest.exchange("/api/resenas/{id}", HttpMethod.PUT,
                new HttpEntity<>(Map.of("calificacion", 4), auth(tokenA, true)), String.class, resenaId);
        assertThat(putPropia.getStatusCode()).isEqualTo(HttpStatus.OK);

        // el encargado lee las de SU restaurante y no las ajenas
        String encargadoToken = login("reg-resena-resto@test.com", "secret1");
        ResponseEntity<String> delEncargado = rest.exchange("/api/resenas?id={id}", HttpMethod.GET,
                new HttpEntity<>(auth(encargadoToken, false)), String.class, restoId);
        assertThat(delEncargado.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(delEncargado.getBody()).contains("muy bueno");

        Long otroResto = registrarResto("reg-resena-resto2@test.com", "RestoAjeno", 10, "CAFE", true);
        ResponseEntity<String> restoAjeno = rest.exchange("/api/resenas?id={id}", HttpMethod.GET,
                new HttpEntity<>(auth(encargadoToken, false)), String.class, otroResto);
        assertThat(restoAjeno.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        // borrar: ajena 403, admin 204
        ResponseEntity<String> deleteAjena = rest.exchange("/api/resenas/{id}", HttpMethod.DELETE,
                new HttpEntity<>(auth(tokenB, false)), String.class, resenaId);
        assertThat(deleteAjena.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        ResponseEntity<String> deleteAdmin = rest.exchange("/api/resenas/{id}", HttpMethod.DELETE,
                new HttpEntity<>(auth(admin, false)), String.class, resenaId);
        assertThat(deleteAdmin.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        // el 204 tiene que venir con la fila borrada de verdad
        assertThat(rest.exchange("/api/resenas/{id}", HttpMethod.GET,
                new HttpEntity<>(auth(admin, false)), String.class, resenaId).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void restaurantesPendientesNoSonPublicos() {
        registrarResto("reg-pendiente@test.com", "RestoPendiente", 5, "MINUTAS", false);
        Long pendienteId = idResto("RestoPendiente");

        // anonimo: como si no existiera
        ResponseEntity<String> anonimo = rest.getForEntity("/api/restaurantes/{id}", String.class, pendienteId);
        assertThat(anonimo.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        // el dueño (rol PENDIENTE) y el admin sí lo ven
        String duenio = login("reg-pendiente@test.com", "secret1");
        ResponseEntity<String> vistoPorDuenio = rest.exchange("/api/restaurantes/{id}", HttpMethod.GET,
                new HttpEntity<>(auth(duenio, false)), String.class, pendienteId);
        assertThat(vistoPorDuenio.getStatusCode()).isEqualTo(HttpStatus.OK);
        ResponseEntity<String> vistoPorAdmin = rest.exchange("/api/restaurantes/{id}", HttpMethod.GET,
                new HttpEntity<>(auth(adminToken(), false)), String.class, pendienteId);
        assertThat(vistoPorAdmin.getStatusCode()).isEqualTo(HttpStatus.OK);

        // el listado por especialidad no lo incluye
        ResponseEntity<String> lista = rest.getForEntity(
                "/api/restaurantes/especialidad?especialidadDeComida=MINUTAS", String.class);
        if (lista.getStatusCode().value() == HttpStatus.OK.value()) {
            assertThat(lista.getBody()).doesNotContain("RestoPendiente");
        } else {
            assertThat(lista.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        }
    }

    @Test
    void elCupoEsPorRestauranteYNoseExcede() {
        String admin = adminToken();
        Long restoA = registrarResto("reg-cupo-a@test.com", "RestoCupoA", 2, "ASIATICA", true);
        Long restoB = registrarResto("reg-cupo-b@test.com", "RestoCupoB", 2, "MINUTAS", true);

        Long cliente = registrarCliente("reg-cupo@test.com", "Fede");
        String token = login("reg-cupo@test.com", "secret1");
        String dia = fecha(31);

        assertThat(crearReserva(token, cliente, restoA, 2, dia, "19:30").getStatusCode())
                .isEqualTo(HttpStatus.CREATED);

        // cupo completo en A
        ResponseEntity<String> excedida = crearReserva(token, cliente, restoA, 1, dia, "20:00");
        assertThat(excedida.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(excedida.getBody()).contains("El cupo maximo es: 2");

        // B de la misma fecha no debería estar afectada por lo reservado en A
        assertThat(crearReserva(token, cliente, restoB, 2, dia, "19:30").getStatusCode())
                .isEqualTo(HttpStatus.CREATED);
        ResponseEntity<String> excedidaB = crearReserva(token, cliente, restoB, 1, dia, "21:00");
        assertThat(excedidaB.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void cartaSoloAceptaPdf() {
        Long restoId = registrarResto("reg-carta@test.com", "RestoCarta", 10, "PARRILLA", true);
        String encargado = login("reg-carta@test.com", "secret1");

        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("archivo", new ByteArrayResource("esto no es un pdf".getBytes()) {
            @Override
            public String getFilename() {
                return "carta.txt";
            }
        });
        form.add("restaurantId", restoId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBearerAuth(encargado);

        ResponseEntity<String> respuesta = rest.postForEntity(
                "/api/carta", new HttpEntity<>(form, headers), String.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(respuesta.getBody()).doesNotContain("error interno");
    }

    @Test
    void unEncargadoNoSeBorraSoloYElAdminNoBorraConHistorial() {
        String admin = adminToken();
        registrarResto("reg-encargado-borrado@test.com", "RestoBorrable", 5, "CAFE", false);
        String encargado = login("reg-encargado-borrado@test.com", "secret1");
        ResponseEntity<Map> quien = rest.postForEntity("/api/auth/login",
                new HttpEntity<>(Map.of("email", "reg-encargado-borrado@test.com", "password", "secret1")),
                Map.class);
        assertThat(quien.getStatusCode()).isEqualTo(HttpStatus.OK);

        Long encargadoId = restaurantRepo.findAll().stream()
                .filter(r -> r.getNombre().equals("RestoBorrable"))
                .map(Restaurant::getUsuario)
                .findFirst()
                .orElseThrow()
                .getId();

        // el encargado no se puede dar de baja a sí mismo
        ResponseEntity<String> autoBorrado = rest.exchange("/api/clientes/{id}", HttpMethod.DELETE,
                new HttpEntity<>(auth(encargado, false)), String.class, encargadoId);
        assertThat(autoBorrado.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        // el admin sí lo puede, y no tira 500 por la FK del restaurante
        ResponseEntity<String> borradoAdmin = rest.exchange("/api/clientes/{id}", HttpMethod.DELETE,
                new HttpEntity<>(auth(admin, false)), String.class, encargadoId);
        assertThat(borradoAdmin.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }
}
