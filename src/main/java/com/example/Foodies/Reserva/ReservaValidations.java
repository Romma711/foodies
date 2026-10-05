package com.example.Foodies.Reserva;

import com.example.Foodies.Exception.BusinessException;
import com.example.Foodies.Exception.NotApprovedException;
import com.example.Foodies.Exception.NotValidCupoException;
import com.example.Foodies.Restaurant.Restaurant;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public class ReservaValidations {

    public static void validateRestaurantIsActive(Restaurant restaurant) throws NotApprovedException{
        if(!restaurant.isAprobado()){
            throw new NotApprovedException("El restaurante no esta aprobado");
        }
    }

    public static void validateCupo(Integer cantidad,Integer reservado, Integer cupoMaximo) throws NotValidCupoException{
        if((cantidad + reservado) > cupoMaximo){
            throw new NotValidCupoException("El cupo maximo es: " + cupoMaximo);
        }
        if(cantidad < 0){
            throw new NotValidCupoException("Cupo invalido");
        }
    }

    /**
     * Valida la fecha del request y la devuelve ya parseada.
     * Lanza BusinessException (400) si falta, viene mal o no cumple las reglas.
     */
    public static LocalDate validateDate(String fecha){
        if (fecha == null || fecha.trim().isEmpty()) {
            throw new BusinessException("La fecha es obligatoria. Debe ser yyyy-MM-dd (ejemplo: 2024-12-25)");
        }

        try {
            LocalDate fechaReserva = LocalDate.parse(fecha.trim());
            LocalDate hoy = LocalDate.now();

            if (hoy.isAfter(fechaReserva)){
                throw new BusinessException("Fecha no válida: No puede ser anterior al día de hoy");
            }

            LocalDate maxFecha = hoy.plusMonths(3);
            if (fechaReserva.isAfter(maxFecha)) {
                throw new BusinessException("No se pueden hacer reservas con más de 3 meses de anticipación");
            }

            return fechaReserva;

        } catch (DateTimeParseException e) {
            throw new BusinessException("Formato de fecha inválido. Debe ser yyyy-MM-dd (ejemplo: 2024-12-25)");
        }
    }

    public static void validateTime(String hora) throws BusinessException {
        if (hora == null || hora.trim().isEmpty()) {
            throw new BusinessException("La hora es obligatoria");
        }

        try {
            // Esto validará que el formato sea HH:mm
            java.time.LocalTime.parse(hora.trim());
        } catch (DateTimeParseException e) {
            throw new BusinessException("Formato de hora inválido. Debe ser HH:mm (ejemplo: 19:30)");
        }
    }
}
