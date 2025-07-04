package com.example.Foodies.Reserva;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Date;
import java.util.List;

public interface ReservaRepository extends JpaRepository<Reserva,Long> {
    List<Reserva> findAllByFechaReserva(Date fechaReserva);

    List<Reserva> findAllByCliente_Id(Long clienteId);

    List<Reserva> findAllByRestaurant_Id(Long restaurantId);
}
