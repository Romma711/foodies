package com.example.Foodies.Restaurant;

import com.example.Foodies.Enums.EspecialidadDeComida;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {



    List<Restaurant> findByEspecialidad(EspecialidadDeComida especialidadDeComida);

    List<Restaurant> findByAprobadoTrue();
    List<Restaurant> findByAprobadoFalse();

    /**
     * Lee el restaurante bloqueando la fila (SELECT ... FOR UPDATE).
     * Se usa para validar el cupo de reservas sin que dos requests
     * simultaneas aprueben la misma ultima plaza.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Restaurant r where r.id = :id")
    Optional<Restaurant> findByIdForUpdate(@Param("id") Long id);

}
