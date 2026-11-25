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
    /** Public listing by specialty: approved restaurants that were not deactivated by an admin (Phase 3). */
    List<Restaurant> findByEspecialidadAndAprobadoTrueAndActivoTrue(EspecialidadDeComida especialidadDeComida);
    /** Public listing by specialty, also hiding restaurants whose manager account was deactivated. */
    List<Restaurant> findByEspecialidadAndAprobadoTrueAndActivoTrueAndUsuario_ActivoTrue(EspecialidadDeComida especialidadDeComida);

    List<Restaurant> findByAprobadoTrue();
    /** Public catalog: approved restaurants that were not deactivated by an admin (Phase 3). */
    List<Restaurant> findByAprobadoTrueAndActivoTrue();
    /** Public catalog, also hiding restaurants whose manager account was deactivated. */
    List<Restaurant> findByAprobadoTrueAndActivoTrueAndUsuario_ActivoTrue();
    List<Restaurant> findByAprobadoFalse();

    Optional<Restaurant> findByUsuario_Email(String email);

    /**
     * Loads the restaurant with a pessimistic write lock until the transaction ends. Every capacity
     * check of a restaurant takes it first, so two bookings cannot both see the same free seats.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Restaurant r where r.id = :id")
    Optional<Restaurant> bloquearParaReservar(@Param("id") Long id);

}
