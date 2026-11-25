package com.example.Foodies.Turno.Dtos;

import com.example.Foodies.Espacio.TipoEspacio;

/** Free seats of one space in one shift (or in the whole day for a restaurant without shifts). */
public record EspacioDisponibleDTO(Long idEspacio, String nombre, TipoEspacio tipo, Integer libres) {
}
