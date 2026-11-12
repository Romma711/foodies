package com.example.Foodies.Espacio.Dtos;

import com.example.Foodies.Espacio.TipoEspacio;

/**
 * Espacio que el cliente puede elegir al reservar. No incluye la capacidad: al cliente solo le
 * importa si hay lugar. {@code hayLugar} es {@code null} cuando no se pidió fecha y cantidad.
 */
public record EspacioPublicoDTO(Long id, TipoEspacio tipo, String nombre, Boolean hayLugar) {
}
