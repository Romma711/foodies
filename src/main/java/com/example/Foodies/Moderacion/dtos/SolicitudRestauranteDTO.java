package com.example.Foodies.Moderacion.dtos;

import com.example.Foodies.Enums.EspecialidadDeComida;
import com.example.Foodies.Enums.EstadoRestaurante;

import java.time.LocalDateTime;

/**
 * Solicitud de alta de un restaurante, tal como la ve el administrador.
 *
 * @param restaurantId  id del restaurante
 * @param nombre        nombre del restaurante
 * @param ubicacion     dirección declarada en el registro
 * @param especialidad  tipo de comida
 * @param cupoMaximo    capacidad declarada
 * @param estado        estado actual de la solicitud
 * @param usuarioId     id de la cuenta del encargado
 * @param email         email del encargado, usado para avisos
 * @param telefono      teléfono de contacto del encargado
 * @param fechaRegistro alta de la cuenta del encargado; {@code null} en cuentas anteriores a esa columna
 */
public record SolicitudRestauranteDTO(
        Long restaurantId,
        String nombre,
        String ubicacion,
        EspecialidadDeComida especialidad,
        Integer cupoMaximo,
        EstadoRestaurante estado,
        Long usuarioId,
        String email,
        String telefono,
        LocalDateTime fechaRegistro
) {
}
