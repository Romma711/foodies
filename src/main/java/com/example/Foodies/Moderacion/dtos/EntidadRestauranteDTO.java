package com.example.Foodies.Moderacion.dtos;

import com.example.Foodies.Enums.EspecialidadDeComida;
import com.example.Foodies.Enums.EstadoRestaurante;

/**
 * Restaurante tal como lo ve el administrador en "Gestión de Entidades".
 *
 * @param id           id del restaurante
 * @param nombre       nombre
 * @param ubicacion    dirección
 * @param especialidad tipo de comida
 * @param estado       estado de la solicitud de alta
 * @param strikes      sanciones acumuladas
 * @param activo       {@code false} si está dado de baja (no aparece en el catálogo público)
 * @param usuarioId    id de la cuenta del encargado
 * @param email        email del encargado
 */
public record EntidadRestauranteDTO(
        Long id,
        String nombre,
        String ubicacion,
        EspecialidadDeComida especialidad,
        EstadoRestaurante estado,
        Integer strikes,
        Boolean activo,
        Long usuarioId,
        String email
) {
}
