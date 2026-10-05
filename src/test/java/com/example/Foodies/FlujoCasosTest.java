package com.example.Foodies;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cobertura de casos del flujo principal: registro y login, aprobación de
 * restaurantes, visibilidad, CRUD con permisos, reservas (casos inválidos y
 * permisos), reseñas y clientes.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FlujoCasosTest extends ApiTestSupport {

    // ---------------------------------------------------------------- auth

    @Test
    void registroDeClienteValidaLosDatos() {
        Map<String, Object> emailInvalido = new HashMap<>();
        emailInvalido.put("nombre", "N");
        emailInvalido.put("apellido", "A");
        emailInvalido.put("email", "no-es-un-email");
        emailInvalido.put("password", "secret1");
        emailInvalido.put("telefono", "111");
        assertThat(rest.postForEntity("/api/auth/register/cliente",
                new HttpEntity<>(emailInvalido, auth(null, true)), String.class)
                .getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        Map<String, Object> passwordCorta = new HashMap<>(emailInvalido);
        passwordCorta.put("email", "casos-pass@test.com");
        passwordCorta.put("password", "1234");
        assertThat(rest.postForEntity("/api/auth/register/cliente",
                new HttpEntity<>(passwordCorta, auth(null, true)), String.class)
                .getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        Map<String, Object> sinCampos = new HashMap<>();
        assertThat(rest.postForEntity("/api/auth/register/cliente",
                new HttpEntity<>(sinCampos, auth(null, true)), String.class)
                .getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        assertThat(registrarCliente("casos-registro@test.com", "Ana")).isNotNull();
    }

    @Test
    void registroDuplicadoResponde409() {
        registrarCliente("casos-duplicado@test.com", "Ana");

        ResponseEntity<String> repetido = rest.postForEntity("/api/auth/register/cliente",
                new HttpEntity<>(Map.of("nombre", "Ana", "apellido", "Apellido",
                        "email", "casos-duplicado@test.com", "password", "secret1", "telefono", "111"),
                        auth(null, true)),
                String.class);

        assertThat(repetido.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(repetido.getBody()).contains("Este email esta en uso");
    }

    @Test
    void endpointsProtegidosRechazanTokensInvalidos() {
        ResponseEntity<String> sinToken = get(null, "/api/clientes");
        assertThat(sinToken.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        ResponseEntity<String> tokenFalso = rest.exchange("/api/clientes", HttpMethod.GET,
                new HttpEntity<>(auth("token-inventado-123", false)), String.class);
        assertThat(tokenFalso.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        registrarCliente("casos-cliente-admin@test.com", "Leo");
        String token = login("casos-cliente-admin@test.com", "secret1");

        ResponseEntity<String> clienteEnAdmin = get(token, "/api/admin/requests");
        assertThat(clienteEnAdmin.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        ResponseEntity<String> clienteEnListadoClientes = get(token, "/api/clientes");
        assertThat(clienteEnAdmin.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(clienteEnListadoClientes.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ------------------------------------------------------------- admin

    @Test
    void aprobacionDeRestaurante() {
        String admin = adminToken();
        registrarResto("casos-aprobar@test.com", "RestoPorAprobar", 5, "MINUTAS", false);
        Long pendienteId = idResto("RestoPorAprobar");

        ResponseEntity<String> pedidos = get(admin, "/api/admin/requests");
        assertThat(pedidos.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(pedidos.getBody()).contains("RestoPorAprobar");

        ResponseEntity<String> aprobado = rest.exchange("/api/admin/approved/{id}", HttpMethod.PUT,
                new HttpEntity<>(auth(admin, false)), String.class, idUsuario("casos-aprobar@test.com"));
        assertThat(aprobado.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(aprobado.getBody()).contains("ROLE_ENCARGADO");

        // aprobado: pasa a ser publico
        assertThat(get(null, "/api/restaurantes/{id}", pendienteId).getStatusCode())
                .isEqualTo(HttpStatus.OK);

        // aprobar a un usuario sin restaurante -> 400 (antes 500)
        registrarCliente("casos-sinresto@test.com", "Rita");
        ResponseEntity<String> sinRestaurante = rest.exchange("/api/admin/approved/{id}", HttpMethod.PUT,
                new HttpEntity<>(auth(admin, false)), String.class, idUsuario("casos-sinresto@test.com"));
        assertThat(sinRestaurante.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(sinRestaurante.getBody()).contains("no tiene restaurante asociado");

        // id inexistente -> 404
        ResponseEntity<String> inexistente = rest.exchange("/api/admin/approved/{id}", HttpMethod.PUT,
                new HttpEntity<>(auth(admin, false)), String.class, 999999L);
        assertThat(inexistente.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ------------------------------------------------------- restaurantes

    @Test
    void restaurantePendienteSoloLoVeanSuDuenioYElAdmin() {
        registrarResto("casos-oculto@test.com", "RestoOculto", 5, "PESCADOS", false);
        Long ocultoId = idResto("RestoOculto");
        registrarCliente("casos-que-no-ve@test.com", "Omar");
        String observador = login("casos-que-no-ve@test.com", "secret1");

        assertThat(get(null, "/api/restaurantes/{id}", ocultoId).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(get(observador, "/api/restaurantes/{id}", ocultoId).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(get(login("casos-oculto@test.com", "secret1"), "/api/restaurantes/{id}", ocultoId)
                .getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(get(adminToken(), "/api/restaurantes/{id}", ocultoId).getStatusCode())
                .isEqualTo(HttpStatus.OK);

        ResponseEntity<String> listado = get(null, "/api/restaurantes");
        if (listado.getStatusCode().value() == HttpStatus.OK.value()) {
            assertThat(listado.getBody()).doesNotContain("RestoOculto");
        } else {
            assertThat(listado.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        }

        ResponseEntity<String> especialidad = rest.getForEntity(
                "/api/restaurantes/especialidad?especialidadDeComida=PESCADOS", String.class);
        if (especialidad.getStatusCode().value() == HttpStatus.OK.value()) {
            assertThat(especialidad.getBody()).doesNotContain("RestoOculto");
        }
    }

    @Test
    void crudDeRestauranteRespetaLosPermisos() {
        Long restoA = registrarResto("casos-encargado-a@test.com", "RestoDelA", 10, "PARRILLA", true);
        Long restoB = registrarResto("casos-encargado-b@test.com", "RestoDelB", 10, "CAFE", true);
        String tokenA = login("casos-encargado-a@test.com", "secret1");
        String tokenB = login("casos-encargado-b@test.com", "secret1");
        registrarCliente("casos-cliente-crud@test.com", "Clara");
        String tokenCliente = login("casos-cliente-crud@test.com", "secret1");

        // el dueño edita el suyo
        ResponseEntity<String> propio = rest.exchange("/api/restaurantes/{id}", HttpMethod.PATCH,
                new HttpEntity<>(Map.of("ubicacion", "Calle Nueva 1"), auth(tokenA, true)),
                String.class, restoA);
        assertThat(propio.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(propio.getBody()).contains("Calle Nueva 1");

        // otro encargado no puede
        ResponseEntity<String> ajeno = rest.exchange("/api/restaurantes/{id}", HttpMethod.PATCH,
                new HttpEntity<>(Map.of("ubicacion", "Hackeada"), auth(tokenB, true)),
                String.class, restoA);
        assertThat(ajeno.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        // un cliente ni siquiera entra al endpoint
        ResponseEntity<String> cliente = rest.exchange("/api/restaurantes/{id}", HttpMethod.PATCH,
                new HttpEntity<>(Map.of("ubicacion", "Hackeada"), auth(tokenCliente, true)),
                String.class, restoA);
        assertThat(cliente.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        assertThat(get(tokenA, "/api/restaurantes/{id}", restoA).getStatusCode())
                .isEqualTo(HttpStatus.OK);

        // detalle inexistente
        assertThat(get(tokenA, "/api/restaurantes/{id}", 999999L).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);

        // borrar resto con historial -> 409 (antes borraba reservas y reseñas en cascada)
        Long restoConHistorial = registrarResto("casos-resto-historial@test.com", "RestoConHistorial",
                10, "ASIATICA", true);
        registrarCliente("casos-cliente-historial@test.com", "Fede");
        String tokenFede = login("casos-cliente-historial@test.com", "secret1");
        assertThat(crearReserva(tokenFede, idUsuario("casos-cliente-historial@test.com"),
                restoConHistorial, 1, fecha(20), "19:30").getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<String> borrarConHistorial = rest.exchange("/api/restaurantes/{id}",
                HttpMethod.DELETE,
                new HttpEntity<>(auth(login("casos-resto-historial@test.com", "secret1"), false)),
                String.class, restoConHistorial);
        assertThat(borrarConHistorial.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(borrarConHistorial.getBody()).contains("reservas o reseñas asociadas");
        assertThat(get(null, "/api/restaurantes/{id}", restoConHistorial).getStatusCode())
                .isEqualTo(HttpStatus.OK);

        // borrar resto sin historial por su dueño -> 204
        Long restoLibre = registrarResto("casos-resto-libre@test.com", "RestoLibre", 10, "PASTAS", true);
        ResponseEntity<String> borrarLibre = rest.exchange("/api/restaurantes/{id}", HttpMethod.DELETE,
                new HttpEntity<>(auth(login("casos-resto-libre@test.com", "secret1"), false)),
                String.class, restoLibre);
        assertThat(borrarLibre.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(get(null, "/api/restaurantes/{id}", restoLibre).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);

        // borrar el resto de otro -> 403
        ResponseEntity<String> borrarAjeno = rest.exchange("/api/restaurantes/{id}", HttpMethod.DELETE,
                new HttpEntity<>(auth(tokenB, false)), String.class, restoA);
        assertThat(borrarAjeno.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ------------------------------------------------------------ reservas

    @Test
    void reservasRechazanEntradasInvalidas() {
        String admin = adminToken();
        registrarCliente("casos-reservas-invalidas@test.com", "Hugo");
        String token = login("casos-reservas-invalidas@test.com", "secret1");
        Long clienteId = idUsuario("casos-reservas-invalidas@test.com");
        Long resto = registrarResto("casos-reservas-resto@test.com", "RestoReservas", 10, "PASTAS", true);

        // sin fecha
        Map<String, Object> sinFecha = new HashMap<>();
        sinFecha.put("cantidad", 1);
        sinFecha.put("horarioLlegada", "19:30");
        sinFecha.put("idUsuario", clienteId);
        sinFecha.put("idRestaurant", resto);
        ResponseEntity<String> rSinFecha = rest.exchange("/api/reservas", HttpMethod.POST,
                new HttpEntity<>(sinFecha, auth(token, true)), String.class);
        assertThat(rSinFecha.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(rSinFecha.getBody()).contains("La fecha es obligatoria");

        // sin cantidad
        Map<String, Object> sinCantidad = new HashMap<>(sinFecha);
        sinCantidad.remove("cantidad");
        sinCantidad.put("fechaReserva", fecha(10));
        ResponseEntity<String> rSinCantidad = rest.exchange("/api/reservas", HttpMethod.POST,
                new HttpEntity<>(sinCantidad, auth(token, true)), String.class);
        assertThat(rSinCantidad.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(rSinCantidad.getBody()).contains("La cantidad es obligatoria");

        // sin restaurante
        Map<String, Object> sinResto = new HashMap<>(sinCantidad);
        sinResto.remove("idRestaurant");
        ResponseEntity<String> rSinResto = rest.exchange("/api/reservas", HttpMethod.POST,
                new HttpEntity<>(sinResto, auth(token, true)), String.class);
        assertThat(rSinResto.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(rSinResto.getBody()).contains("El restaurante es obligatorio");

        // fecha pasada
        assertThat(crearReserva(token, clienteId, resto, 1, fecha(-1), "19:30").getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        // mas de 3 meses
        String demasiadoLejos = LocalDate.now().plusMonths(5)
                .format(DateTimeFormatter.ISO_LOCAL_DATE);
        ResponseEntity<String> rLejos = crearReserva(token, clienteId, resto, 1, demasiadoLejos, "19:30");
        assertThat(rLejos.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(rLejos.getBody()).contains("3 meses");

        // hora invalida
        assertThat(crearReserva(token, clienteId, resto, 1, fecha(10), "25:99").getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        Map<String, Object> sinHora = new HashMap<>(sinCantidad);
        sinHora.put("cantidad", 1);
        sinHora.put("horarioLlegada", null);
        ResponseEntity<String> rSinHora = rest.exchange("/api/reservas", HttpMethod.POST,
                new HttpEntity<>(sinHora, auth(token, true)), String.class);
        assertThat(rSinHora.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(rSinHora.getBody()).contains("La hora es obligatoria");

        // formato de fecha invalido
        assertThat(crearReserva(token, clienteId, resto, 1, "10/12/2027", "19:30").getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        // restaurante inexistente
        assertThat(crearReserva(token, clienteId, 999999L, 1, fecha(10), "19:30").getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);

        // restaurante pendiente de aprobacion
        Long pendiente = registrarResto("casos-reservas-pendiente@test.com", "RestoSinAprobar",
                10, "CAFE", false);
        ResponseEntity<String> rPendiente = crearReserva(token, clienteId, pendiente, 1, fecha(10), "19:30");
        assertThat(rPendiente.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(rPendiente.getBody()).contains("no esta aprobado");

        // el admin si puede reservar por un cliente
        assertThat(crearReserva(admin, clienteId, resto, 1, fecha(11), "19:30").getStatusCode())
                .isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void reservasPermisosEstadosYListados() {
        Long resto = registrarResto("casos-res-permisos@test.com", "RestoPermisosRes", 10, "MINUTAS", true);
        Long encargadoId = idUsuario("casos-res-permisos@test.com");
        String tokenEncargado = login("casos-res-permisos@test.com", "secret1");

        registrarCliente("casos-res-a@test.com", "Ana");
        String tokenA = login("casos-res-a@test.com", "secret1");
        Long idA = idUsuario("casos-res-a@test.com");

        registrarCliente("casos-res-b@test.com", "Bruno");
        String tokenB = login("casos-res-b@test.com", "secret1");
        Long idB = idUsuario("casos-res-b@test.com");

        String admin = adminToken();
        String dia = fecha(15);

        assertThat(crearReserva(tokenA, idA, resto, 1, dia, "19:30").getStatusCode())
                .isEqualTo(HttpStatus.CREATED);
        ResponseEntity<String> creadaB = crearReserva(tokenB, idB, resto, 1, dia, "20:00");
        assertThat(creadaB.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Long reservaB = Long.valueOf(creadaB.getBody().replaceAll(".*\"id\":(\\d+).*", "$1"));

        // lectura de una reserva
        assertThat(get(tokenA, "/api/reservas/{id}", reservaB).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(get(tokenB, "/api/reservas/{id}", reservaB).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(get(admin, "/api/reservas/{id}", reservaB).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(get(tokenA, "/api/reservas/{id}", 999999L).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);

        // listado general: solo admin
        assertThat(get(tokenA, "/api/reservas").getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(get(admin, "/api/reservas").getStatusCode()).isEqualTo(HttpStatus.OK);

        // reservas por usuario: solo el propio o admin
        assertThat(get(tokenA, "/api/reservas/usuario/{id}", idB).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(get(tokenB, "/api/reservas/usuario/{id}", idB).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(get(admin, "/api/reservas/usuario/{id}", idB).getStatusCode())
                .isEqualTo(HttpStatus.OK);

        // reservas por restaurante: el dueño si, un cliente no
        assertThat(get(tokenEncargado, "/api/reservas/restaurante/{id}", resto).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(get(tokenA, "/api/reservas/restaurante/{id}", resto).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);

        // editar cantidad: ajena 403, propia 200
        ResponseEntity<String> patchAjena = rest.exchange("/api/reservas/{id}", HttpMethod.PUT,
                new HttpEntity<>(Map.of("cantidad", 2), auth(tokenA, true)), String.class, reservaB);
        assertThat(patchAjena.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        ResponseEntity<String> patchPropia = rest.exchange("/api/reservas/{id}", HttpMethod.PUT,
                new HttpEntity<>(Map.of("cantidad", 2), auth(tokenB, true)), String.class, reservaB);
        assertThat(patchPropia.getStatusCode()).isEqualTo(HttpStatus.OK);

        // cambiar el estado: el cliente no puede, si el encargado
        ResponseEntity<String> estadoCliente = rest.exchange("/api/reservas/{id}", HttpMethod.PUT,
                new HttpEntity<>(Map.of("estadoReserva", "ACEPTADA"), auth(tokenB, true)),
                String.class, reservaB);
        assertThat(estadoCliente.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        ResponseEntity<String> estadoEncargado = rest.exchange("/api/reservas/{id}", HttpMethod.PUT,
                new HttpEntity<>(Map.of("estadoReserva", "ACEPTADA"), auth(tokenEncargado, true)),
                String.class, reservaB);
        assertThat(estadoEncargado.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(estadoEncargado.getBody()).contains("ACEPTADA");

        // borrar: ajena 403, propia 204
        ResponseEntity<String> deleteAjena = rest.exchange("/api/reservas/{id}", HttpMethod.DELETE,
                new HttpEntity<>(auth(tokenA, false)), String.class, reservaB);
        assertThat(deleteAjena.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        ResponseEntity<String> deletePropia = rest.exchange("/api/reservas/{id}", HttpMethod.DELETE,
                new HttpEntity<>(auth(tokenB, false)), String.class, reservaB);
        assertThat(deletePropia.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(get(tokenB, "/api/reservas/{id}", reservaB).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ------------------------------------------------------------ reseñas

    @Test
    void resenasValidanCalificacionComentarioYDuplicado() {
        Long resto = registrarResto("casos-resenas-resto@test.com", "RestoResenas", 10, "PARRILLA", true);
        registrarCliente("casos-resenas-cliente@test.com", "Ivana");
        String token = login("casos-resenas-cliente@test.com", "secret1");
        String admin = adminToken();

        // sin restaurantId (antes 500)
        ResponseEntity<String> sinRestaurante = rest.exchange("/api/resenas", HttpMethod.POST,
                new HttpEntity<>(Map.of("comentario", "bueno", "calificacion", 3), auth(token, true)),
                String.class);
        assertThat(sinRestaurante.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(sinRestaurante.getBody()).contains("El restaurante es obligatorio");

        // sin comentario
        ResponseEntity<String> sinComentario = rest.exchange("/api/resenas", HttpMethod.POST,
                new HttpEntity<>(Map.of("calificacion", 3, "restaurantId", resto),
                        auth(token, true)), String.class);
        assertThat(sinComentario.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(sinComentario.getBody()).contains("El comentario es obligatorio");

        // calificacion fuera de rango
        ResponseEntity<String> calificacionCero = rest.exchange("/api/resenas", HttpMethod.POST,
                new HttpEntity<>(Map.of("comentario", "malo", "calificacion", 0, "restaurantId", resto),
                        auth(token, true)), String.class);
        assertThat(calificacionCero.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        ResponseEntity<String> calificacionSeis = rest.exchange("/api/resenas", HttpMethod.POST,
                new HttpEntity<>(Map.of("comentario", "malo", "calificacion", 6, "restaurantId", resto),
                        auth(token, true)), String.class);
        assertThat(calificacionSeis.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        // reseña valida
        ResponseEntity<String> creada = rest.exchange("/api/resenas", HttpMethod.POST,
                new HttpEntity<>(Map.of("comentario", "muy rico", "calificacion", 5,
                        "restaurantId", resto), auth(token, true)), String.class);
        assertThat(creada.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Long resenaId = Long.valueOf(creada.getBody().replaceAll(".*\"id\":(\\d+).*", "$1"));

        // duplicada
        ResponseEntity<String> duplicada = rest.exchange("/api/resenas", HttpMethod.POST,
                new HttpEntity<>(Map.of("comentario", "otra vez", "calificacion", 4,
                        "restaurantId", resto), auth(token, true)), String.class);
        assertThat(duplicada.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(duplicada.getBody()).contains("ya realizó una reseña");

        // lectura
        assertThat(get(token, "/api/resenas/{id}", resenaId).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(get(token, "/api/resenas/{id}", 999999L).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(get(token, "/api/resenas?id={id}", resto).getStatusCode()).isEqualTo(HttpStatus.OK);

        // una reseña mas del admin no entra (el admin no reseña)
        ResponseEntity<String> reseñaAdmin = rest.exchange("/api/resenas", HttpMethod.POST,
                new HttpEntity<>(Map.of("comentario", "de admin", "calificacion", 5,
                        "restaurantId", resto), auth(admin, true)), String.class);
        assertThat(reseñaAdmin.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ----------------------------------------------------------- clientes

    @Test
    void clientesLecturaEscrituraYBorrado() {
        String admin = adminToken();
        registrarCliente("casos-cliente-uno@test.com", "Julia");
        String token = login("casos-cliente-uno@test.com", "secret1");
        Long id = idUsuario("casos-cliente-uno@test.com");

        // lectura inexistente
        assertThat(get(admin, "/api/clientes/{id}", 999999L).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        // primero valida que el id sea tuyo, asi que un id que no es el tuyo da 403
        assertThat(get(token, "/api/clientes/{id}", 999999L).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);

        // el listado lo ve solo el admin y trae al cliente recien creado
        ResponseEntity<String> listado = get(admin, "/api/clientes");
        assertThat(listado.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listado.getBody()).contains("casos-cliente-uno@test.com");

        // el cliente ve el suyo
        ResponseEntity<String> propio = get(token, "/api/clientes/{id}", id);
        assertThat(propio.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(propio.getBody()).contains("casos-cliente-uno@test.com");

        // patch con body vacio: no pisa nada
        ResponseEntity<String> patchVacio = rest.exchange("/api/clientes/{id}", HttpMethod.PATCH,
                new HttpEntity<>(Map.of(), auth(token, true)), String.class, id);
        assertThat(patchVacio.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(patchVacio.getBody()).contains("\"nombre\":\"Julia\"");

        // un admin si puede editar a cualquiera
        ResponseEntity<String> patchAdmin = rest.exchange("/api/clientes/{id}", HttpMethod.PATCH,
                new HttpEntity<>(Map.of("nombre", "Julia Editada"), auth(admin, true)),
                String.class, id);
        assertThat(patchAdmin.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(patchAdmin.getBody()).contains("Julia Editada");

        // borrado sin historial
        ResponseEntity<String> borrado = rest.exchange("/api/clientes/{id}", HttpMethod.DELETE,
                new HttpEntity<>(auth(admin, false)), String.class, id);
        assertThat(borrado.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(get(admin, "/api/clientes/{id}", id).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }
}
