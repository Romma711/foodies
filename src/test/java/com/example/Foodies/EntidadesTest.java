package com.example.Foodies;

import com.example.Foodies.Resena.Resena;
import com.example.Foodies.Resena.ResenaRepository;
import com.example.Foodies.Reserva.Reserva;
import com.example.Foodies.Reserva.ReservaRepository;
import com.example.Foodies.Restaurant.Restaurant;
import com.example.Foodies.Usuario.Usuario;
import com.example.Foodies.Usuario.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Las entidades usan Lombok @Data con relaciones bidireccionales
 * (Restaurant.carta <-> Carta.restaurant, Restaurant.usuario <-> Usuario.restaurant,
 * etc). Con los getters generados, toString()/equals() se llamaban entre si y
 * terminaban en StackOverflowError en cuanto se imprimia una entidad.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EntidadesTest extends ApiTestSupport {

    @Autowired private UsuarioRepository usuarios;
    @Autowired private ReservaRepository reservas;
    @Autowired private ResenaRepository resenas;

    private void sembrarDatos(String tag) {
        Long resto = registrarResto("entidades-resto-" + tag + "@test.com", "RestoEntidades" + tag, 10, "CAFE", true);
        registrarCliente("entidades-cliente-" + tag + "@test.com", "Ana");
        String token = login("entidades-cliente-" + tag + "@test.com", "secret1");
        Long clienteId = idUsuario("entidades-cliente-" + tag + "@test.com");

        crearReserva(token, clienteId, resto, 2, fecha(10), "19:30");

        rest.exchange("/api/resenas", org.springframework.http.HttpMethod.POST,
                new org.springframework.http.HttpEntity<>(
                        Map.of("comentario", "buena", "calificacion", 4, "restaurantId", resto),
                        auth(token, true)),
                String.class);
    }

    @Test
    void imprimirEntidadesNoExplota() {
        sembrarDatos("a");

        assertThat(restaurantRepo.findAll()).isNotEmpty();
        for (Restaurant r : restaurantRepo.findAll()) {
            assertThatCode(() -> r.toString()).doesNotThrowAnyException();
        }
        for (Usuario u : usuarios.findAll()) {
            assertThatCode(() -> u.toString()).doesNotThrowAnyException();
        }
        for (Reserva res : reservas.findAll()) {
            assertThatCode(() -> res.toString()).doesNotThrowAnyException();
        }
        for (Resena re : resenas.findAll()) {
            assertThatCode(() -> re.toString()).doesNotThrowAnyException();
        }
    }

    @Test
    void toStringNoMuestraLaPassword() {
        sembrarDatos("b");

        Usuario conPassword = usuarios.findByEmail("entidades-cliente-b@test.com").orElseThrow();
        assertThat(conPassword.toString())
                .contains("entidades-cliente-b@test.com")
                .doesNotContain("secret1");
    }
}