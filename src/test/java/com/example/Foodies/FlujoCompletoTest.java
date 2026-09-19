package com.example.Foodies;

import com.example.Foodies.Reserva.dtos.ReservaDetailDTO;
import com.example.Foodies.Restaurant.Restaurant;
import com.example.Foodies.Restaurant.RestaurantRepository;
import com.example.Foodies.Usuario.dtos.UsuarioDetailDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FlujoCompletoTest {

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private RestaurantRepository restaurantRepo;

    private String login(String email, String password) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> req = new HttpEntity<>(Map.of("email", email, "password", password), headers);
        ResponseEntity<Map> resp = rest.postForEntity("/api/auth/login", req, Map.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        String token = (String) resp.getBody().get("Token");
        assertThat(token).startsWith("Bearer ");
        return token.substring("Bearer ".length());
    }

    @Test
    void flujoCompletoDiezVeces() {
        String adminToken = login("admin@foodies.com", "admin123");

        for (int i = 1; i <= 10; i++) {
            // 1. Registrar cliente
            ResponseEntity<UsuarioDetailDTO> reg = rest.postForEntity(
                    "/api/auth/register/cliente",
                    Map.of(
                            "nombre", "Nombre" + i,
                            "apellido", "Apellido" + i,
                            "email", "cliente" + i + "@test.com",
                            "password", "secret1",
                            "telefono", "11" + i),
                    UsuarioDetailDTO.class);
            assertThat(reg.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(reg.getBody().rol()).isEqualTo("ROLE_CLIENTE");
            Long clienteId = reg.getBody().id();

            // 2. Login del cliente
            String clienteToken = login("cliente" + i + "@test.com", "secret1");

            // 3. Registrar restaurante (encargado pendiente)
            ResponseEntity<UsuarioDetailDTO> regRes = rest.postForEntity(
                    "/api/auth/register/restaurante",
                    Map.of(
                            "email", "encargado" + i + "@test.com",
                            "password", "secret1",
                            "nombreRestaurante", "Resto" + i,
                            "direccion", "Direccion " + i,
                            "telefono", "22" + i,
                            "especialidadDeComida", "PASTAS",
                            "cupoMaximo", 10),
                    UsuarioDetailDTO.class);
            assertThat(regRes.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(regRes.getBody().rol()).isEqualTo("ROLE_PENDIENTE");
            Long encargadoId = regRes.getBody().id();

            // 4. Admin aprueba al encargado
            HttpHeaders adminHeaders = new HttpHeaders();
            adminHeaders.setBearerAuth(adminToken);
            ResponseEntity<UsuarioDetailDTO> aprobado = rest.exchange(
                    "/api/admin/approved/{id}",
                    HttpMethod.PUT,
                    new HttpEntity<>(adminHeaders),
                    UsuarioDetailDTO.class,
                    encargadoId);
            assertThat(aprobado.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(aprobado.getBody().rol()).isEqualTo("ROLE_ENCARGADO");

            String nombreResto = "Resto" + i;
            Long restaurantId = restaurantRepo.findAll().stream()
                    .filter(r -> r.getNombre().equals(nombreResto))
                    .findFirst()
                    .map(Restaurant::getId)
                    .orElseThrow();

            // 5. Crear reserva con el token del cliente
            HttpHeaders clientHeaders = new HttpHeaders();
            clientHeaders.setContentType(MediaType.APPLICATION_JSON);
            clientHeaders.setBearerAuth(clienteToken);

            String fecha = LocalDate.now().plusDays(i).format(DateTimeFormatter.ISO_LOCAL_DATE);
            HttpEntity<Map<String, Object>> reservaReq = new HttpEntity<>(Map.of(
                    "cantidad", 1,
                    "fechaReserva", fecha,
                    "horarioLlegada", "19:30",
                    "idUsuario", clienteId,
                    "idRestaurant", restaurantId), clientHeaders);

            ResponseEntity<ReservaDetailDTO> reservaResp = rest.exchange(
                    "/api/reservas",
                    HttpMethod.POST,
                    reservaReq,
                    ReservaDetailDTO.class);
            assertThat(reservaResp.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(reservaResp.getBody().cantidad()).isEqualTo(1);
            assertThat(reservaResp.getBody().nombreUsuario()).isEqualTo("Nombre" + i);
            assertThat(reservaResp.getBody().estadoReserva().name()).isEqualTo("PENDIENTE");

            // 6. Listar reservas del usuario
            ResponseEntity<List> reservasDelUsuario = rest.exchange(
                    "/api/reservas/usuario/{id}",
                    HttpMethod.GET,
                    new HttpEntity<>(clientHeaders),
                    List.class,
                    clienteId);
            assertThat(reservasDelUsuario.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(reservasDelUsuario.getBody()).hasSize(1);
        }
    }
}