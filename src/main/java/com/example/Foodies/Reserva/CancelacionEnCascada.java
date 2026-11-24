package com.example.Foodies.Reserva;

import com.example.Foodies.Enums.EstadoReserva;
import com.example.Foodies.ListaEspera.ListaEsperaService;
import com.example.Foodies.Moderacion.AvisosReserva;
import com.example.Foodies.Restaurant.Restaurant;
import com.example.Foodies.Usuario.Usuario;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Cancels the future active reservations of an account that is deactivated (D7): a client, a
 * restaurant or the manager of a restaurant. Reservations whose arrival already passed are left as
 * they are, so attendance can still be read from them.
 */
@Service
public class CancelacionEnCascada {

    private final ReservaRepository reservaRepo;
    private final Clock clock;
    private final AvisosReserva avisos;
    private final ListaEsperaService listaEspera;

    public CancelacionEnCascada(ReservaRepository reservaRepo, Clock clock, AvisosReserva avisos,
                                ListaEsperaService listaEspera) {
        this.reservaRepo = reservaRepo;
        this.clock = clock;
        this.avisos = avisos;
        this.listaEspera = listaEspera;
    }

    /** Future active reservations of the client, now cancelled; each restaurant is told by email. */
    @Transactional
    public List<Reserva> cancelarDelCliente(Usuario cliente) {
        List<Reserva> canceladas = cancelar(
                reservaRepo.findAllByUsuario_IdAndEstadoReservaIn(cliente.getId(), EstadoReserva.ACTIVOS));
        canceladas.forEach(avisos::canceladaPorBajaDelCliente);
        // The freed seats go to the waitlist; a deactivated restaurant has no one to offer them to.
        canceladas.forEach(listaEspera::lugarLiberado);
        return canceladas;
    }

    /** Future active reservations of the restaurant, now cancelled; each client is told by email. */
    @Transactional
    public List<Reserva> cancelarDelRestaurante(Restaurant restaurant) {
        List<Reserva> canceladas = cancelar(
                reservaRepo.findAllByRestaurant_IdAndEstadoReservaIn(restaurant.getId(), EstadoReserva.ACTIVOS));
        canceladas.forEach(avisos::canceladaPorBajaDelRestaurante);
        return canceladas;
    }

    /** True when the restaurant still has active reservations whose arrival has not passed. */
    @Transactional(readOnly = true)
    public boolean tieneReservasFuturas(Restaurant restaurant) {
        return !futuras(reservaRepo.findAllByRestaurant_IdAndEstadoReservaIn(restaurant.getId(), EstadoReserva.ACTIVOS))
                .isEmpty();
    }

    private List<Reserva> cancelar(List<Reserva> activas) {
        List<Reserva> futuras = futuras(activas);
        futuras.forEach(r -> r.setEstadoReserva(EstadoReserva.CANCELADA));
        reservaRepo.saveAll(futuras);
        return futuras;
    }

    private List<Reserva> futuras(List<Reserva> activas) {
        LocalDateTime ahora = LocalDateTime.now(clock);
        return activas.stream().filter(r -> r.llegada() != null && r.llegada().isAfter(ahora)).toList();
    }
}
