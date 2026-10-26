package com.example.Foodies.ADMIN.dtos;

/**
 * Fila del ranking de restaurantes con más reservas. Se construye desde JPQL ({@code select new ...}).
 *
 * @param id               id del restaurante
 * @param nombre           nombre del restaurante
 * @param cantidadReservas reservas no canceladas que recibió el restaurante
 */
public record RestauranteReservasDTO(Long id, String nombre, Long cantidadReservas) {
}
