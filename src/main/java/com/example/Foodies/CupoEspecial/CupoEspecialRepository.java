package com.example.Foodies.CupoEspecial;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CupoEspecialRepository extends JpaRepository<CupoEspecial, Long> {

    Optional<CupoEspecial> findByRestaurant_IdAndFecha(Long restaurantId, LocalDate fecha);

    List<CupoEspecial> findAllByRestaurant_IdAndFechaGreaterThanEqualOrderByFechaAsc(Long restaurantId, LocalDate desde);
}
