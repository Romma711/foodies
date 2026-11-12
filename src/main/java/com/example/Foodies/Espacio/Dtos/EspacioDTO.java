package com.example.Foodies.Espacio.Dtos;

import com.example.Foodies.Espacio.Espacio;
import com.example.Foodies.Espacio.TipoEspacio;

/** Espacio tal como lo ve el encargado en el panel, con su capacidad. */
public record EspacioDTO(Long id, TipoEspacio tipo, String nombre, Integer capacidad, Boolean activo) {

    public static EspacioDTO from(Espacio espacio) {
        return new EspacioDTO(espacio.getId(), espacio.getTipo(), espacio.getNombre(),
                espacio.getCapacidad(), espacio.getActivo());
    }
}
