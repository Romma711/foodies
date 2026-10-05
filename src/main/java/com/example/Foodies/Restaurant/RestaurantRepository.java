package com.example.Foodies.Restaurant;

import com.example.Foodies.Enums.EspecialidadDeComida;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {



    /** Solo los aprobados, y solo los de esa especialidad (los pendientes no se publican) */
    Page<Restaurant> findByEspecialidadAndAprobadoTrue(
            EspecialidadDeComida especialidadDeComida, Pageable pageable);

    Page<Restaurant> findByAprobadoTrue(Pageable pageable);
    Page<Restaurant> findByAprobadoFalse(Pageable pageable);

    /**
     * Lee el restaurante bloqueando la fila (SELECT ... FOR UPDATE).
     * Se usa para validar el cupo de reservas sin que dos requests
     * simultaneas aprueben la misma ultima plaza.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Restaurant r where r.id = :id")
    Optional<Restaurant> findByIdForUpdate(@Param("id") Long id);

}
