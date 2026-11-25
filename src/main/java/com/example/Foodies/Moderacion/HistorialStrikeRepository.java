package com.example.Foodies.Moderacion;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistorialStrikeRepository extends JpaRepository<HistorialStrike, Long> {

    List<HistorialStrike> findAllByUsuario_IdOrderByFechaDescIdDesc(Long usuarioId);

    List<HistorialStrike> findAllByRestaurant_IdOrderByFechaDescIdDesc(Long restaurantId);
}
