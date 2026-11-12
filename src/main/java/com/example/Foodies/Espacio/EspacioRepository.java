package com.example.Foodies.Espacio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EspacioRepository extends JpaRepository<Espacio, Long> {

    List<Espacio> findAllByRestaurant_IdOrderByIdAsc(Long restaurantId);

    List<Espacio> findAllByRestaurant_IdAndActivoTrueOrderByIdAsc(Long restaurantId);

    boolean existsByRestaurant_Id(Long restaurantId);

    /** Capacidad total del restaurante: la suma de sus espacios activos. */
    @Query("select coalesce(sum(e.capacidad), 0) from Espacio e where e.restaurant.id = :restaurantId and e.activo = true")
    int capacidadTotal(@Param("restaurantId") Long restaurantId);
}
