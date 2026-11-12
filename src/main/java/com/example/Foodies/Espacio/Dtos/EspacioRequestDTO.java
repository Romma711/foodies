package com.example.Foodies.Espacio.Dtos;

import com.example.Foodies.Espacio.TipoEspacio;

/** Cuerpo de `POST /api/restaurantes/me/espacios` y `PUT /api/restaurantes/me/espacios/{id}`. */
public record EspacioRequestDTO(TipoEspacio tipo, String nombre, Integer capacidad) {
}
