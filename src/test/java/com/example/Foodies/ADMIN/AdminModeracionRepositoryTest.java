package com.example.Foodies.ADMIN;

import com.example.Foodies.ADMIN.dtos.ResenaModeracionDTO;
import com.example.Foodies.Enums.EspecialidadDeComida;
import com.example.Foodies.Enums.Role;
import com.example.Foodies.Resena.Resena;
import com.example.Foodies.Restaurant.Restaurant;
import com.example.Foodies.Usuario.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Runs the moderation query against H2 to prove ordering, limit and the flattened fields. */
@DataJpaTest
class AdminModeracionRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private AdminModeracionRepository repo;

    private int secuencia = 0;

    @Test
    void resenasRecientesDevuelveLasUltimasPrimeroConSuAutorYRestaurante() {
        Usuario ana = usuario("Ana", "Gómez");
        Restaurant ochiwan = restaurante("Ochiwan");
        Resena primera = resena(ochiwan, ana, "Primera visita", 3);
        Resena segunda = resena(ochiwan, ana, "Segunda visita", 4);
        Resena tercera = resena(ochiwan, ana, "Tercera visita", 5);

        List<ResenaModeracionDTO> recientes = repo.resenasRecientes(PageRequest.of(0, 2));

        assertThat(recientes).extracting(ResenaModeracionDTO::id)
                .containsExactly(tercera.getId(), segunda.getId());
        assertThat(recientes).extracting(ResenaModeracionDTO::id).doesNotContain(primera.getId());
        ResenaModeracionDTO ultima = recientes.get(0);
        assertThat(ultima.comentario()).isEqualTo("Tercera visita");
        assertThat(ultima.calificacion()).isEqualTo(5);
        assertThat(ultima.nombreUsuario()).isEqualTo("Ana Gómez");
        assertThat(ultima.restauranteId()).isEqualTo(ochiwan.getId());
        assertThat(ultima.nombreRestaurante()).isEqualTo("Ochiwan");
    }

    @Test
    void resenasRecientesToleraAutoresSinNombre() {
        Usuario sinNombre = usuario(null, null);
        resena(restaurante("Ochiwan"), sinNombre, "Comentario anónimo", 2);

        ResenaModeracionDTO resena = repo.resenasRecientes(PageRequest.of(0, 5)).get(0);

        assertThat(resena.nombreUsuario()).isBlank();
    }

    private Usuario usuario(String nombre, String apellido) {
        Usuario usuario = new Usuario();
        usuario.setNombre(nombre);
        usuario.setApellido(apellido);
        usuario.setEmail("usuario" + (secuencia++) + "@test.com");
        usuario.setPassword("secret");
        usuario.setTelefono("1100000000");
        usuario.setRol(Role.ROLE_CLIENTE);
        return em.persistAndFlush(usuario);
    }

    private Restaurant restaurante(String nombre) {
        Restaurant restaurant = new Restaurant();
        restaurant.setNombre(nombre);
        restaurant.setCupoMaximo(20);
        restaurant.setUbicacion("Calle " + nombre);
        restaurant.setAprobado(true);
        restaurant.setEspecialidad(EspecialidadDeComida.ASIATICA);
        restaurant.setUsuario(usuario("Encargado", nombre));
        return em.persistAndFlush(restaurant);
    }

    private Resena resena(Restaurant restaurant, Usuario autor, String comentario, int calificacion) {
        Resena resena = new Resena();
        resena.setComentario(comentario);
        resena.setCalificacion(calificacion);
        resena.setRestaurant(restaurant);
        resena.setUsuario(autor);
        return em.persistAndFlush(resena);
    }
}
