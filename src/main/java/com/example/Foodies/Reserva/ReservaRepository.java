package com.example.Foodies.Reserva;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface ReservaRepository extends JpaRepository<Reserva,Long> {

    /** Cupo: solo las reservas de ESA fecha y de ESE restaurante */
    List<Reserva> findAllByFechaReservaAndRestaurant_Id(LocalDate fechaReserva, Long restaurantId);

    List<Reserva> findAllByUsuario_Id(Long usuarioId);

    boolean existsByUsuario_Id(Long usuarioId);

    List<Reserva> findAllByRestaurant_Id(Long restaurantId);

    boolean existsByRestaurant_Id(Long restaurantId);
}
