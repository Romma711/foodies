package com.example.Foodies.ADMIN;

import com.example.Foodies.ADMIN.dtos.RestauranteCalificacionDTO;
import com.example.Foodies.ADMIN.dtos.RestauranteReservasDTO;
import com.example.Foodies.ADMIN.dtos.UsuarioResenasDTO;
import com.example.Foodies.Enums.EspecialidadDeComida;
import com.example.Foodies.Enums.EstadoReserva;
import com.example.Foodies.Enums.Role;
import com.example.Foodies.Resena.Resena;
import com.example.Foodies.Reserva.Reserva;
import com.example.Foodies.Restaurant.Restaurant;
import com.example.Foodies.Usuario.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs every metrics query against the H2 test database so the JPQL, the grouping and the
 * ordering are proven on real SQL, not on mocks.
 */
@DataJpaTest
class AdminMetricasRepositoryTest {

    private static final PageRequest TOP_5 = PageRequest.of(0, 5);

    @Autowired
    private TestEntityManager em;

    @Autowired
    private AdminMetricasRepository repo;

    private int secuencia = 0;

    @Test
    void topRestaurantesMejorCalificadosOrdenaPorPromedioYLimitaACinco() {
        Usuario cliente = usuario("Ana");
        Restaurant a = restaurante("A", true);
        Restaurant b = restaurante("B", true);
        Restaurant c = restaurante("C", true);
        Restaurant d = restaurante("D", true);
        Restaurant e = restaurante("E", true);
        Restaurant f = restaurante("F", true);
        Restaurant noAprobado = restaurante("Pendiente", false);
        resenas(a, cliente, 5, 5);
        resenas(b, cliente, 5);
        resenas(c, cliente, 4, 4);
        resenas(d, cliente, 3);
        resenas(e, cliente, 2);
        resenas(f, cliente, 1);
        resenas(noAprobado, cliente, 5, 5, 5);

        List<RestauranteCalificacionDTO> top = repo.topRestaurantesMejorCalificados(TOP_5);

        assertThat(top).extracting(RestauranteCalificacionDTO::nombre)
                .containsExactly("A", "B", "C", "D", "E");
        assertThat(top.get(0).promedio()).isEqualTo(5.0);
        assertThat(top.get(0).cantidadResenas()).isEqualTo(2L);
    }

    @Test
    void topRestaurantesConMasReservasIgnoraLasCanceladas() {
        Usuario cliente = usuario("Ana");
        Restaurant a = restaurante("A", true);
        Restaurant b = restaurante("B", true);
        reservas(a, cliente, EstadoReserva.ACEPTADA, EstadoReserva.PENDIENTE);
        reservas(b, cliente, EstadoReserva.ACEPTADA, EstadoReserva.CANCELADA,
                EstadoReserva.CANCELADA, EstadoReserva.CANCELADA);

        List<RestauranteReservasDTO> top = repo.topRestaurantesConMasReservas(EstadoReserva.CANCELADA, TOP_5);

        assertThat(top).extracting(RestauranteReservasDTO::nombre).containsExactly("A", "B");
        assertThat(top).extracting(RestauranteReservasDTO::cantidadReservas).containsExactly(2L, 1L);
    }

    @Test
    void topUsuariosConMasResenasOrdenaPorCantidad() {
        Usuario ana = usuario("Ana");
        Usuario luis = usuario("Luis");
        Restaurant a = restaurante("A", true);
        Restaurant b = restaurante("B", true);
        resenas(a, luis, 4);
        resenas(a, ana, 5);
        resenas(b, ana, 3);

        List<UsuarioResenasDTO> top = repo.topUsuariosConMasResenas(TOP_5);

        assertThat(top).extracting(UsuarioResenasDTO::nombre).containsExactly("Ana", "Luis");
        assertThat(top).extracting(UsuarioResenasDTO::cantidadResenas).containsExactly(2L, 1L);
    }

    @Test
    void contarUsuariosRegistradosEntreIncluyeElInicioYExcluyeElFin() {
        Usuario dentro = usuario("Dentro");
        Usuario inicioExacto = usuario("Inicio");
        Usuario finExacto = usuario("Fin");
        Usuario sinFecha = usuario("Legado");
        fechaRegistro(dentro, LocalDateTime.of(2026, 10, 15, 12, 0));
        fechaRegistro(inicioExacto, LocalDateTime.of(2026, 10, 1, 0, 0));
        fechaRegistro(finExacto, LocalDateTime.of(2026, 11, 1, 0, 0));
        fechaRegistro(sinFecha, null);

        long total = repo.contarUsuariosRegistradosEntre(
                LocalDateTime.of(2026, 10, 1, 0, 0),
                LocalDateTime.of(2026, 11, 1, 0, 0));

        assertThat(total).isEqualTo(2L);
    }

    @Test
    void fechaRegistroSeCompletaAlCrearElUsuario() {
        Usuario nuevo = usuario("Nuevo");

        assertThat(nuevo.getFechaRegistro()).isNotNull();
    }

    private Usuario usuario(String nombre) {
        Usuario usuario = new Usuario();
        usuario.setNombre(nombre);
        usuario.setApellido("Test");
        usuario.setEmail(nombre.toLowerCase() + (secuencia++) + "@test.com");
        usuario.setPassword("secret");
        usuario.setTelefono("1100000000");
        usuario.setRol(Role.ROLE_CLIENTE);
        return em.persistAndFlush(usuario);
    }

    private Restaurant restaurante(String nombre, boolean aprobado) {
        Usuario encargado = usuario("Encargado" + nombre);
        Restaurant restaurant = new Restaurant();
        restaurant.setNombre(nombre);
        restaurant.setCupoMaximo(20);
        restaurant.setUbicacion("Calle " + nombre);
        restaurant.setAprobado(aprobado);
        restaurant.setEspecialidad(EspecialidadDeComida.PASTAS);
        restaurant.setUsuario(encargado);
        return em.persistAndFlush(restaurant);
    }

    private void resenas(Restaurant restaurant, Usuario autor, int... calificaciones) {
        for (int calificacion : calificaciones) {
            Resena resena = new Resena();
            resena.setComentario("Comentario de prueba");
            resena.setCalificacion(calificacion);
            resena.setRestaurant(restaurant);
            resena.setUsuario(autor);
            em.persist(resena);
        }
        em.flush();
    }

    private void reservas(Restaurant restaurant, Usuario cliente, EstadoReserva... estados) {
        for (EstadoReserva estado : estados) {
            Reserva reserva = new Reserva();
            reserva.setCantidad(2);
            reserva.setFechaReserva(new Date());
            reserva.setHorariollegada(LocalTime.of(20, 0));
            reserva.setEstadoReserva(estado);
            reserva.setRestaurant(restaurant);
            reserva.setUsuario(cliente);
            em.persist(reserva);
        }
        em.flush();
    }

    /** Overrides the creation timestamp, which Hibernate only writes on insert. */
    private void fechaRegistro(Usuario usuario, LocalDateTime fecha) {
        em.getEntityManager()
                .createQuery("update Usuario u set u.fechaRegistro = :fecha where u.id = :id")
                .setParameter("fecha", fecha)
                .setParameter("id", usuario.getId())
                .executeUpdate();
        em.clear();
    }
}
