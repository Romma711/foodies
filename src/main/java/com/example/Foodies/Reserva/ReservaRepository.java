package com.example.Foodies.Reserva;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Date;
import java.util.List;

public interface ReservaRepository extends JpaRepository<Reserva,Long> {
    List<Reserva> findAllByRestaurant_IdAndFechaReserva(Long restaurantId, Date fechaReserva);

    List<Reserva> findAllByUsuario_Id(Long usuarioId);

    List<Reserva> findAllByRestaurant_Id(Long restaurantId);
}
