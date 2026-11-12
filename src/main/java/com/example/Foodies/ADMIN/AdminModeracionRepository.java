package com.example.Foodies.ADMIN;

import com.example.Foodies.ADMIN.dtos.ResenaModeracionDTO;
import com.example.Foodies.Resena.Resena;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.Repository;

import java.util.List;

/**
 * Consultas de solo lectura para la moderación de reseñas del administrador.
 * <p>
 * Repositorio propio del módulo ADMIN para no modificar {@code ResenaRepository}. El borrado no vive
 * acá: el panel reutiliza {@code DELETE /api/resenas/{id}}, que ya permite borrar a {@code ROLE_ADMIN}.
 */
public interface AdminModeracionRepository extends Repository<Resena, Long> {

    /**
     * Reseñas más recientes de toda la plataforma, de la más nueva a la más vieja.
     * <p>
     * La entidad {@link Resena} no guarda fecha de creación, así que se ordena por {@code id}
     * descendente: el id es autoincremental, por lo que un id mayor siempre es una reseña posterior.
     * <p>
     * Une cada reseña con su autor y su restaurante y devuelve una fila plana. El nombre del autor se
     * arma con {@code trim(concat(coalesce(nombre, ''), ' ', coalesce(apellido, '')))} para que un
     * usuario sin nombre ni apellido no rompa la concatenación y llegue como texto vacío.
     *
     * @param texto         patrón {@code like} en minúsculas (por ejemplo {@code %sushi%}) que se busca en
     *                      el comentario, el autor y el restaurante; {@code null} no filtra
     * @param calificacion  calificación exacta (1 a 5); {@code null} no filtra
     * @param restauranteId solo reseñas de ese restaurante; {@code null} no filtra
     * @param pagina        página y cantidad de filas (Spring Data lo traduce a {@code LIMIT/OFFSET})
     * @return reseñas recientes con autor y restaurante
     */
    @Query("""
            select new com.example.Foodies.ADMIN.dtos.ResenaModeracionDTO(
                r.id,
                r.comentario,
                r.calificacion,
                trim(concat(coalesce(u.nombre, ''), ' ', coalesce(u.apellido, ''))),
                rest.id,
                rest.nombre)
            from Resena r
            join r.usuario u
            join r.restaurant rest
            where (:texto is null
                   or lower(r.comentario) like :texto
                   or lower(trim(concat(coalesce(u.nombre, ''), ' ', coalesce(u.apellido, '')))) like :texto
                   or lower(rest.nombre) like :texto)
              and (:calificacion is null or r.calificacion = :calificacion)
              and (:restauranteId is null or rest.id = :restauranteId)
            order by r.id desc
            """)
    List<ResenaModeracionDTO> resenasRecientes(@Param("texto") String texto,
                                               @Param("calificacion") Integer calificacion,
                                               @Param("restauranteId") Long restauranteId,
                                               Pageable pagina);
}
