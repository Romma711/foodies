package com.example.Foodies.Moderacion;

import com.example.Foodies.Enums.EstadoRestaurante;
import com.example.Foodies.Moderacion.dtos.SolicitudRestauranteDTO;
import com.example.Foodies.Restaurant.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Consultas de moderación sobre restaurantes (Fase 3).
 *
 * <p>Repositorio propio para no modificar {@code RestaurantRepository}, que usa el resto del equipo.</p>
 */
public interface ModeracionRestaurantRepository extends JpaRepository<Restaurant, Long> {

    /**
     * Solicitudes en el estado indicado, de la más antigua a la más nueva.
     *
     * @param estado estado a filtrar, normalmente {@link EstadoRestaurante#PENDIENTE}
     * @return una fila por restaurante con los datos de contacto del encargado
     */
    @Query("""
            select new com.example.Foodies.Moderacion.dtos.SolicitudRestauranteDTO(
                r.id, r.nombre, r.ubicacion, r.especialidad, r.cupoMaximo, r.estado,
                u.id, u.email, u.telefono, u.fechaRegistro)
            from Restaurant r
            join r.usuario u
            where r.estado = :estado
            order by r.id
            """)
    List<SolicitudRestauranteDTO> findSolicitudes(@Param("estado") EstadoRestaurante estado);

    /**
     * Completa el estado de las filas creadas antes de que existiera la columna.
     *
     * @param aprobado valor de {@code aprobado} de las filas a completar
     * @param estado   estado que reciben esas filas
     * @return cantidad de filas actualizadas
     */
    @Modifying
    @Query("update Restaurant r set r.estado = :estado where r.estado is null and r.aprobado = :aprobado")
    int completarEstado(@Param("aprobado") boolean aprobado, @Param("estado") EstadoRestaurante estado);
}
