package com.example.Foodies.Reserva.dtos;

import com.example.Foodies.Enums.EstadoReserva;
import com.example.Foodies.Reserva.Reserva;
import com.example.Foodies.Usuario.Usuario;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;

/** A reservation as the restaurant manager sees it: who comes, when and how many. */
public record ReservaPanelDTO(
        Long id,
        Integer cantidad,
        LocalDate fecha,
        LocalTime horarioLlegada,
        EstadoReserva estado,
        String nombreCliente,
        String telefonoCliente
) {

    public static ReservaPanelDTO from(Reserva reserva) {
        Usuario cliente = reserva.getUsuario();
        String nombre = cliente == null ? "" : (nullToEmpty(cliente.getNombre()) + " " + nullToEmpty(cliente.getApellido())).strip();
        return new ReservaPanelDTO(
                reserva.getId(),
                reserva.getCantidad(),
                toLocalDate(reserva.getFechaReserva()),
                reserva.getHorariollegada(),
                reserva.getEstadoReserva(),
                nombre,
                cliente == null ? null : cliente.getTelefono());
    }

    /** `fechaReserva` may come back from JPA as `java.sql.Date`/`Timestamp`, whose `toInstant` is not portable. */
    private static LocalDate toLocalDate(Date fecha) {
        return fecha == null ? null : new Date(fecha.getTime()).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
