package com.example.Foodies.Resena;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResenaRepository extends JpaRepository<Resena,Long> {
    boolean existsByCliente_IdAndRestaurant_Id(Long clienteId, Long restaurantId);

    List<Resena> findByRestaurant_Id(Long restaurantId);


    List<Resena> findByCliente_Id(Long clienteId);
}
