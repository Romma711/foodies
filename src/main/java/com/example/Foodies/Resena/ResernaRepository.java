package com.example.Foodies.Resena;

import com.example.Foodies.Cliente.Cliente;
import com.example.Foodies.Restaurant.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResernaRepository extends JpaRepository<Resena,Long> {
    boolean existsByClienteAndRestaurant(Cliente cliente, Restaurant restaurant);
}
