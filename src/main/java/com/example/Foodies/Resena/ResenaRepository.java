package com.example.Foodies.Resena;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResenaRepository extends JpaRepository<Resena,Long> {
    boolean existsByUsuario_IdAndRestaurant_Id(Long usuarioId, Long restaurantId);

    Page<Resena> findByRestaurant_Id(Long restaurantId, Pageable pageable);


    Page<Resena> findByUsuario_Id(Long usuarioId, Pageable pageable);

    boolean existsByUsuario_Id(Long usuarioId);

    boolean existsByRestaurant_Id(Long restaurantId);
}
