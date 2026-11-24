package com.example.Foodies.Reserva;

import com.example.Foodies.Exception.BusinessException;
import com.example.Foodies.Exception.NotApprovedException;
import com.example.Foodies.Exception.NotValidCupoException;
import com.example.Foodies.Restaurant.Restaurant;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;

public class ReservaValidations {

    public static void validateRestaurantIsActive(Restaurant restaurant) throws NotApprovedException{
        if(!restaurant.isAprobado()){
            throw new NotApprovedException("El restaurante no esta aprobado");
        }
    }

    public static void validateCupo(Integer cantidad,Integer reservado, Integer cupoMaximo) throws NotValidCupoException{
        if((cantidad + reservado) > cupoMaximo){
            // No se informa la capacidad: al cliente solo le importa si hay lugar o no.
            throw new NotValidCupoException("No hay lugar para %d %s ese día. Probá con otra fecha o menos personas."
                    .formatted(cantidad, cantidad == 1 ? "persona" : "personas"));
        }
        if(cantidad < 0){
            throw new NotValidCupoException("Cupo invalido");
        }
    }

    /** The date is not before {@code hoy} and at most 3 months ahead. */
    public static void validateDate(String fecha, LocalDate hoy){
        if (fecha == null || fecha.isBlank()) {
            throw new BusinessException("La fecha es obligatoria");
        }
        try {
            LocalDate fechaReserva = LocalDate.parse(fecha.trim());

            if (hoy.isAfter(fechaReserva)){
                throw new BusinessException("Fecha no válida: No puede ser anterior al día de hoy");
            }

            LocalDate maxFecha = hoy.plusMonths(3);
            if (fechaReserva.isAfter(maxFecha)) {
                throw new BusinessException("No se pueden hacer reservas con más de 3 meses de anticipación");
            }

        } catch (DateTimeParseException e) {
            throw new BusinessException("Formato de fecha inválido. Debe ser yyyy-MM-dd (ejemplo: 2024-12-25)");
        }
    }

    /** The reservation date and arrival time cannot be earlier than {@code ahora}. */
    public static void validateNotPast(LocalDate fecha, LocalTime hora, LocalDateTime ahora) {
        if (LocalDateTime.of(fecha, hora).isBefore(ahora)) {
            throw new BusinessException("La fecha y hora de la reserva no pueden ser anteriores a este momento");
        }
    }

    public static void validateTime(String hora) throws BusinessException {
        if (hora == null || hora.trim().isEmpty()) {
            throw new BusinessException("La hora es obligatoria");
        }

        try {
            // Esto validará que el formato sea HH:mm
            LocalTime.parse(hora.trim());
        } catch (DateTimeParseException e) {
            throw new BusinessException("Formato de hora inválido. Debe ser HH:mm (ejemplo: 19:30)");
        }
    }
}
