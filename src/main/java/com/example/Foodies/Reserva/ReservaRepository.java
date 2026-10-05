package com.example.Foodies.Reserva;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.Foodies.Enums.EstadoReserva;

import java.time.LocalDate;
import java.util.List;

public interface ReservaRepository extends JpaRepository<Reserva,Long> {

    /** Cupo: solo las reservas de ESA fecha y de ESE restaurante */
    List<Reserva> findAllByFechaReservaAndRestaurant_Id(LocalDate fechaReserva, Long restaurantId);

    Page<Reserva> findByUsuario_Id(Long usuarioId, Pageable pageable);

    boolean existsByUsuario_Id(Long usuarioId);

    Page<Reserva> findByRestaurant_Id(Long restaurantId, Pageable pageable);

    boolean existsByRestaurant_Id(Long restaurantId);

    /**
     * Una sola reserva viva por usuario, restaurante y fecha.
     * Las canceladas no cuentan: si cancelaste, podés volver a reservar ese día.
     */
    boolean existsByUsuario_IdAndRestaurant_IdAndFechaReservaAndEstadoReservaNot(
            Long usuarioId, Long restaurantId, LocalDate fechaReserva, EstadoReserva estado);
}
