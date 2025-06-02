package com.example.Foodies.Restaurant.Dtos;

import com.example.Foodies.Resena.dtos.ResenaDetailDTO;
import com.example.Foodies.Reserva.dtos.ReservaDetailDTO;
import com.example.Foodies.Usuario.dtos.UsuarioDetailDTO;

import java.util.List;

public record RestaurantDetailDTO(
        Long id,
        String nombre,
        Integer cupoMaximo,
        String ubicacion,

) {
}
