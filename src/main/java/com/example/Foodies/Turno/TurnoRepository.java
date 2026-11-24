package com.example.Foodies.Turno;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.DayOfWeek;
import java.util.List;

public interface TurnoRepository extends JpaRepository<Turno, Long> {

    List<Turno> findAllByRestaurant_IdAndActivoTrueOrderByDiaAscInicioAsc(Long restaurantId);

    List<Turno> findAllByRestaurant_IdAndDiaAndActivoTrueOrderByInicioAsc(Long restaurantId, DayOfWeek dia);

    boolean existsByRestaurant_IdAndActivoTrue(Long restaurantId);
}
