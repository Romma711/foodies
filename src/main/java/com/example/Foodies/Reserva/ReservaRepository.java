package com.example.Foodies.Reserva;

import com.example.Foodies.Enums.EstadoReserva;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;

import java.util.Date;
import java.util.List;

public interface ReservaRepository extends JpaRepository<Reserva,Long> {
    List<Reserva> findAllByRestaurant_IdAndFechaReserva(Long restaurantId, Date fechaReserva);

    List<Reserva> findAllByUsuario_Id(Long usuarioId);

    List<Reserva> findAllByUsuario_IdAndEstadoReservaIn(Long usuarioId, Collection<EstadoReserva> estados);

    List<Reserva> findAllByRestaurant_IdAndEstadoReservaIn(Long restaurantId, Collection<EstadoReserva> estados);

    List<Reserva> findAllByRestaurant_Id(Long restaurantId);

    List<Reserva> findAllByEstadoReservaAndFechaReservaLessThanEqual(EstadoReserva estado, Date hasta);

    List<Reserva> findAllByEspacio_IdAndFechaReserva(Long espacioId, Date fechaReserva);

    List<Reserva> findAllByEspacio_IdAndFechaReservaGreaterThanEqual(Long espacioId, Date desde);
}
