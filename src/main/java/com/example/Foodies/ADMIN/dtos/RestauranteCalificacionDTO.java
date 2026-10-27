package com.example.Foodies.ADMIN.dtos;

/**
 * Fila del ranking de restaurantes mejor calificados.
 * <p>
 * Se construye directamente desde JPQL ({@code select new ...}), por eso los tipos coinciden con lo
 * que devuelven las funciones de agregación: {@code AVG} produce {@link Double} y {@code COUNT}
 * produce {@link Long}.
 *
 * @param id              id del restaurante
 * @param nombre          nombre del restaurante
 * @param promedio        promedio de calificación de sus reseñas, de 1.0 a 5.0
 * @param cantidadResenas cantidad de reseñas que forman ese promedio
 */
public record RestauranteCalificacionDTO(Long id, String nombre, Double promedio, Long cantidadResenas) {
}
