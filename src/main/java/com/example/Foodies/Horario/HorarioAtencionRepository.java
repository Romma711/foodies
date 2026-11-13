package com.example.Foodies.Horario;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface HorarioAtencionRepository extends JpaRepository<HorarioAtencion, Long> {

    List<HorarioAtencion> findAllByRestaurant_Id(Long restaurantId);

    @Modifying
    @Query("delete from HorarioAtencion h where h.restaurant.id = :restaurantId")
    void borrarDelRestaurante(@Param("restaurantId") Long restaurantId);
}
