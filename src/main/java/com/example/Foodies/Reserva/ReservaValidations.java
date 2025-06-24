package com.example.Foodies.Reserva;

import com.example.Foodies.Exception.BusinessException;
import com.example.Foodies.Exception.NotApprovedException;
import com.example.Foodies.Exception.NotValidCupoException;
import com.example.Foodies.Restaurant.Restaurant;
import org.springframework.data.jpa.convert.threeten.Jsr310JpaConverters;

import javax.swing.text.DateFormatter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Date;
import java.util.logging.SimpleFormatter;

public class ReservaValidations {

    public static void validateRestaurantIsActive(Restaurant restaurant){
        if(!restaurant.isAprobado()){
            throw new NotApprovedException("El restaurante no esta aprobado");
        }
    }

    public static void validateCupo(Integer cantidad,Integer reservado, Integer cupoMaximo){
        if((cantidad + reservado) > cupoMaximo){
            throw new NotValidCupoException("El cupo maximo es: " + cupoMaximo);
        }
        if(cantidad < 0){
            throw new NotValidCupoException("Cupo invalido");
        }
    }

    public static void validateDate(String fecha){
        if (LocalDate.parse(fecha).isBefore(LocalDate.now())){
            throw new BusinessException("Fecha no valida: No puede ser antes del dia de hoy");
        }

    }
}
