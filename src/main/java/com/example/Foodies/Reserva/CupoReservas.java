package com.example.Foodies.Reserva;

import com.example.Foodies.Espacio.Espacio;
import com.example.Foodies.Espacio.EspacioService;
import com.example.Foodies.Exception.EntityNotFoundException;
import com.example.Foodies.Restaurant.Restaurant;
import com.example.Foodies.Restaurant.RestaurantRepository;
import com.example.Foodies.Turno.Turno;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Predicate;

/**
 * Capacity of a space on a day, counting only active reservations (see {@link
 * com.example.Foodies.Enums.EstadoReserva#esActiva()}). Callers take {@link #bloquear} first, inside
 * their transaction, so concurrent bookings of the same restaurant are checked one after the other.
 */
@Component
public class CupoReservas {

    private final ReservaRepository reservaRepo;
    private final RestaurantRepository restaurantRepo;
    private final EspacioService espacioService;

    public CupoReservas(ReservaRepository reservaRepo, RestaurantRepository restaurantRepo,
                        EspacioService espacioService) {
        this.reservaRepo = reservaRepo;
        this.restaurantRepo = restaurantRepo;
        this.espacioService = espacioService;
    }

    /** Pessimistic lock on the restaurant until the current transaction ends. */
    public Restaurant bloquear(Long restaurantId) {
        return restaurantRepo.bloquearParaReservar(restaurantId)
                .orElseThrow(() -> new EntityNotFoundException("Restaurante no encontrado"));
    }

    /** Capacity that applies that day in the space (special capacity or the normal one). */
    public int cupo(Espacio espacio, LocalDate fecha) {
        return espacioService.cupoDelDia(espacio, fecha);
    }

    /**
     * People of the other active reservations of the space that day, in {@code turno} when the
     * restaurant works with shifts (null = the whole day).
     */
    public int ocupadas(Espacio espacio, LocalDate fecha, Turno turno, Long excluirId) {
        return personas(otras(espacio, fecha, turno, excluirId), r -> r.getEstadoReserva().esActiva());
    }

    /** People of the other accepted reservations of the space in that day or shift (for auto-acceptance). */
    public int aceptadas(Espacio espacio, LocalDate fecha, Turno turno, Long excluirId) {
        return personas(otras(espacio, fecha, turno, excluirId),
                r -> r.getEstadoReserva() == com.example.Foodies.Enums.EstadoReserva.ACEPTADA);
    }

    /**
     * @throws com.example.Foodies.Exception.NotValidCupoException if {@code cantidad} does not fit
     */
    public void validar(Espacio espacio, LocalDate fecha, Turno turno, int cantidad, Long excluirId) {
        ReservaValidations.validateCupo(cantidad, ocupadas(espacio, fecha, turno, excluirId), cupo(espacio, fecha));
    }

    private List<Reserva> otras(Espacio espacio, LocalDate fecha, Turno turno, Long excluirId) {
        return reservaRepo.findAllByEspacio_IdAndFechaReserva(espacio.getId(), EspacioService.comoFechaReserva(fecha))
                .stream()
                .filter(r -> excluirId == null || !excluirId.equals(r.getId()))
                .filter(r -> mismoCupo(r, turno))
                .toList();
    }

    /**
     * Same capacity pool: the whole day without shifts; with shifts, the same shift, or for a
     * reservation made before shifts existed, an arrival time inside the shift.
     */
    static boolean mismoCupo(Reserva reserva, Turno turno) {
        if (turno == null) {
            return true;
        }
        if (reserva.getTurno() != null) {
            return reserva.getTurno().getId().equals(turno.getId());
        }
        return reserva.getHorariollegada() != null && turno.incluye(reserva.getHorariollegada());
    }

    private static int personas(List<Reserva> reservas, Predicate<Reserva> filtro) {
        return reservas.stream().filter(filtro).mapToInt(Reserva::getCantidad).sum();
    }
}
