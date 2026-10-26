package com.example.Foodies.ADMIN.dtos;

/**
 * Fila de la tabla de moderación de reseñas del administrador.
 * <p>
 * Aplana en una sola fila los datos de la reseña, su autor y su restaurante para que el panel no
 * tenga que hacer más llamadas. Se construye desde JPQL ({@code select new ...}).
 *
 * @param id                id de la reseña (el mismo que usa {@code DELETE /api/resenas/{id}})
 * @param comentario        texto de la reseña
 * @param calificacion      calificación de 1 a 5
 * @param nombreUsuario     nombre y apellido del autor; vacío si el usuario no los cargó
 * @param restauranteId     id del restaurante reseñado
 * @param nombreRestaurante nombre del restaurante reseñado
 */
public record ResenaModeracionDTO(
        Long id,
        String comentario,
        Integer calificacion,
        String nombreUsuario,
        Long restauranteId,
        String nombreRestaurante
) {
}
