package com.example.Foodies.Moderacion.dtos;

/**
 * Resultado de cambiar la baja lógica de un usuario o restaurante.
 *
 * @param id     id de la entidad
 * @param nombre nombre visible
 * @param activo estado en el que quedó
 */
public record EstadoActivoDTO(Long id, String nombre, boolean activo) {
}
