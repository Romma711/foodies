package com.example.Foodies.Moderacion.dtos;

import com.example.Foodies.Enums.EspecialidadDeComida;
import com.example.Foodies.Enums.EstadoRestaurante;
import com.example.Foodies.Foto.Dtos.FotoDTO;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Solicitud de alta de un restaurante, tal como la ve el administrador.
 *
 * @param restaurantId  id del restaurante
 * @param nombre        nombre del restaurante
 * @param ubicacion     dirección declarada en el registro
 * @param especialidad  tipo de comida
 * @param cupoMaximo    capacidad declarada
 * @param estado        estado actual de la solicitud
 * @param usuarioId     id de la cuenta del encargado
 * @param email         email del encargado, usado para avisos
 * @param telefono      teléfono de contacto del encargado
 * @param fechaRegistro alta de la cuenta del encargado; {@code null} en cuentas anteriores a esa columna
 * @param fotos         fotos cargadas en el registro, para revisarlas antes de aprobar
 */
public record SolicitudRestauranteDTO(
        Long restaurantId,
        String nombre,
        String ubicacion,
        EspecialidadDeComida especialidad,
        Integer cupoMaximo,
        EstadoRestaurante estado,
        Long usuarioId,
        String email,
        String telefono,
        LocalDateTime fechaRegistro,
        List<FotoDTO> fotos
) {

    /** Constructor de la consulta JPQL, que no puede traer las fotos: se agregan con {@link #conFotos}. */
    public SolicitudRestauranteDTO(Long restaurantId, String nombre, String ubicacion,
                                   EspecialidadDeComida especialidad, Integer cupoMaximo, EstadoRestaurante estado,
                                   Long usuarioId, String email, String telefono, LocalDateTime fechaRegistro) {
        this(restaurantId, nombre, ubicacion, especialidad, cupoMaximo, estado, usuarioId, email, telefono,
                fechaRegistro, List.of());
    }

    /**
     * @param fotos fotos del restaurante
     * @return la misma solicitud con sus fotos
     */
    public SolicitudRestauranteDTO conFotos(List<FotoDTO> fotos) {
        return new SolicitudRestauranteDTO(restaurantId, nombre, ubicacion, especialidad, cupoMaximo, estado,
                usuarioId, email, telefono, fechaRegistro, fotos);
    }
}
