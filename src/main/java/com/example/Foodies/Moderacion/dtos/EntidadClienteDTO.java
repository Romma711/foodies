package com.example.Foodies.Moderacion.dtos;

import java.time.LocalDateTime;

/**
 * Cliente tal como lo ve el administrador en "Gestión de Entidades".
 *
 * @param id            id del usuario
 * @param nombre        nombre
 * @param apellido      apellido
 * @param email         email de la cuenta
 * @param telefono      teléfono de contacto
 * @param strikes       sanciones acumuladas
 * @param activo        {@code false} si la cuenta está dada de baja
 * @param fechaRegistro alta de la cuenta; {@code null} en cuentas anteriores a esa columna
 */
public record EntidadClienteDTO(
        Long id,
        String nombre,
        String apellido,
        String email,
        String telefono,
        Integer strikes,
        Boolean activo,
        LocalDateTime fechaRegistro
) {
}
