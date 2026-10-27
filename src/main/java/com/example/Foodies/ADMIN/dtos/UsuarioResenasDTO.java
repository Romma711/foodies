package com.example.Foodies.ADMIN.dtos;

/**
 * Fila del ranking de usuarios que más reseñas escribieron. Se construye desde JPQL ({@code select new ...}).
 * <p>
 * No expone email ni datos de contacto: el panel solo necesita identificar al usuario.
 *
 * @param id              id del usuario
 * @param nombre          nombre del usuario
 * @param apellido        apellido del usuario
 * @param cantidadResenas reseñas escritas por el usuario
 */
public record UsuarioResenasDTO(Long id, String nombre, String apellido, Long cantidadResenas) {
}
