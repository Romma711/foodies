package com.example.Foodies.Moderacion;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Datos de una reserva recién aceptada, copiados de la entidad mientras la sesión sigue abierta
 * para poder enviar el email después del commit.
 *
 * @param email         email del cliente
 * @param nombreCliente nombre con el que se saluda
 * @param restaurante   nombre del restaurante
 * @param direccion     ubicación del restaurante
 * @param fecha         día de la reserva
 * @param hora          horario de llegada
 * @param personas      cantidad de personas
 */
record ReservaConfirmada(String email, String nombreCliente, String restaurante, String direccion,
                         LocalDate fecha, LocalTime hora, int personas) {
}
