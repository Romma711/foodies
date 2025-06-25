package com.example.Foodies.Resena;

import com.example.Foodies.Cliente.Cliente;
import com.example.Foodies.Resena.dtos.ResenaDetailDTO;
import com.example.Foodies.Resena.dtos.ResenaListDTO;
import com.example.Foodies.Restaurant.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResernaRepository extends JpaRepository<Resena,Long> {
    boolean existsByCliente_IdAndRestaurant_Id(Long clienteId, Long restaurantId);

    List<Resena> findByRestaurant_Id(Long restaurantId);


    List<Resena> findByCliente_Id(Long clienteId);
}
