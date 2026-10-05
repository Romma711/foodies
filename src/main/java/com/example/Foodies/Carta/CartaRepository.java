package com.example.Foodies.Carta;

import com.example.Foodies.Restaurant.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartaRepository extends JpaRepository<Carta,Long> {

    Optional<Carta> findByRestaurant(Restaurant restaurant);

    boolean existsByRestaurantId(Long id);

    /**
     * Borrado por JPQL en vez de em.remove(): con el remove de Hibernate la fila
     * sobrevivia al flush (la API devolvia 204 sin borrar nada).
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM Carta c WHERE c.id = :id")
    int borrarPorId(@Param("id") Long id);

}