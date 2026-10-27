package com.example.Foodies.ADMIN;

import com.example.Foodies.ADMIN.dtos.RestauranteCalificacionDTO;
import com.example.Foodies.ADMIN.dtos.RestauranteReservasDTO;
import com.example.Foodies.ADMIN.dtos.UsuarioResenasDTO;
import com.example.Foodies.Enums.EstadoReserva;
import com.example.Foodies.Resena.Resena;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Consultas de solo lectura para el panel de métricas del administrador.
 * <p>
 * Es un repositorio propio del módulo ADMIN para no modificar los repositorios de los demás módulos.
 * Extiende {@link Repository} (sin métodos CRUD) porque solo agrega datos: Spring Data necesita un tipo
 * raíz, pero las consultas JPQL pueden recorrer cualquier entidad.
 * <p>
 * Todas las consultas usan expresiones constructoras ({@code select new ...}) para que la base de datos
 * haga la agregación y solo viajen las filas del ranking, en lugar de cargar entidades completas. El
 * límite del top se pasa con un {@link Pageable} (por ejemplo {@code PageRequest.of(0, 5)}), que Spring
 * Data traduce a {@code LIMIT} en el SQL.
 */
public interface AdminMetricasRepository extends Repository<Resena, Long> {

    /**
     * Restaurantes aprobados ordenados por promedio de calificación.
     * <p>
     * Agrupa las reseñas por restaurante y calcula {@code AVG(calificacion)} y {@code COUNT(reseñas)}.
     * Solo considera restaurantes con {@code aprobado = true}, porque los pendientes no son visibles
     * para los clientes. Ante el mismo promedio gana el que tiene más reseñas (es un promedio más
     * confiable) y luego el nombre, para que el orden sea siempre el mismo. Los restaurantes sin
     * reseñas no aparecen.
     *
     * @param pagina cantidad de filas a devolver
     * @return ranking de mayor a menor promedio
     */
    @Query("""
            select new com.example.Foodies.ADMIN.dtos.RestauranteCalificacionDTO(
                rest.id, rest.nombre, avg(r.calificacion), count(r))
            from Resena r
            join r.restaurant rest
            where rest.aprobado = true
            group by rest.id, rest.nombre
            order by avg(r.calificacion) desc, count(r) desc, rest.nombre asc
            """)
    List<RestauranteCalificacionDTO> topRestaurantesMejorCalificados(Pageable pagina);

    /**
     * Restaurantes ordenados por cantidad de reservas.
     * <p>
     * Agrupa las reservas por restaurante y cuenta las que no están en el estado excluido. El servicio
     * pasa {@link EstadoReserva#CANCELADA}: una reserva cancelada no refleja demanda real. Ante el mismo
     * total se ordena por nombre.
     *
     * @param excluido estado de reserva que no se cuenta
     * @param pagina   cantidad de filas a devolver
     * @return ranking de mayor a menor cantidad de reservas
     */
    @Query("""
            select new com.example.Foodies.ADMIN.dtos.RestauranteReservasDTO(
                rest.id, rest.nombre, count(res))
            from Reserva res
            join res.restaurant rest
            where res.estadoReserva <> :excluido
            group by rest.id, rest.nombre
            order by count(res) desc, rest.nombre asc
            """)
    List<RestauranteReservasDTO> topRestaurantesConMasReservas(
            @Param("excluido") EstadoReserva excluido, Pageable pagina);

    /**
     * Usuarios ordenados por cantidad de reseñas escritas.
     * <p>
     * Agrupa las reseñas por autor y cuenta cuántas tiene cada uno. Ante el mismo total se ordena por
     * apellido y nombre.
     *
     * @param pagina cantidad de filas a devolver
     * @return ranking de mayor a menor cantidad de reseñas
     */
    @Query("""
            select new com.example.Foodies.ADMIN.dtos.UsuarioResenasDTO(
                u.id, u.nombre, u.apellido, count(r))
            from Resena r
            join r.usuario u
            group by u.id, u.nombre, u.apellido
            order by count(r) desc, u.apellido asc, u.nombre asc
            """)
    List<UsuarioResenasDTO> topUsuariosConMasResenas(Pageable pagina);

    /**
     * Cuenta los usuarios dados de alta dentro de un rango de fechas.
     * <p>
     * El rango es semiabierto: incluye {@code desde} y excluye {@code hasta}, así un mes completo se
     * expresa como {@code [primer día del mes, primer día del mes siguiente)} sin perder los registros
     * del último segundo. Los usuarios creados antes de que existiera la columna {@code fecha_registro}
     * tienen el valor en {@code NULL} y no se cuentan.
     *
     * @param desde inicio del rango, inclusive
     * @param hasta fin del rango, exclusivo
     * @return cantidad de usuarios registrados en el rango
     */
    @Query("""
            select count(u)
            from Usuario u
            where u.fechaRegistro >= :desde and u.fechaRegistro < :hasta
            """)
    long contarUsuariosRegistradosEntre(
            @Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);
}
