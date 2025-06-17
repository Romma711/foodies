package com.example.Foodies.Carta;

import com.example.Foodies.Restaurant.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartaRepository extends JpaRepository<Carta,Long> {

    Optional<Carta> findByRestaurant(Restaurant restaurant);

    boolean existsByRestaurantId(Long id);

}
