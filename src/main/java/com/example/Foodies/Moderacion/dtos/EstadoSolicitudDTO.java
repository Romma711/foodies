package com.example.Foodies.Moderacion.dtos;

import com.example.Foodies.Enums.EstadoRestaurante;

/**
 * Resultado de aprobar o rechazar una solicitud.
 *
 * @param restaurantId id del restaurante
 * @param nombre       nombre del restaurante
 * @param estado       estado en el que quedó la solicitud
 */
public record EstadoSolicitudDTO(Long restaurantId, String nombre, EstadoRestaurante estado) {
}
