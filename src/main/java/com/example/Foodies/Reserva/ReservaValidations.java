package com.example.Foodies.Reserva;

import com.example.Foodies.Exception.NotApprovedException;
import com.example.Foodies.Exception.NotValidCupoException;
import com.example.Foodies.Restaurant.Restaurant;

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
    }
}
