package com.example.Foodies.Resena;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResenaRepository extends JpaRepository<Resena,Long> {
    boolean existsByUsuario_IdAndRestaurant_Id(Long usuarioId, Long restaurantId);

    List<Resena> findByRestaurant_Id(Long restaurantId);


    List<Resena> findByUsuario_Id(Long usuarioId);
}
