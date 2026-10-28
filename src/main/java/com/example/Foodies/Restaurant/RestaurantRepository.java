package com.example.Foodies.Restaurant;

import com.example.Foodies.Enums.EspecialidadDeComida;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {



    List<Restaurant> findByEspecialidad(EspecialidadDeComida especialidadDeComida);

    List<Restaurant> findByAprobadoTrue();
    /** Public catalog: approved restaurants that were not deactivated by an admin (Phase 3). */
    List<Restaurant> findByAprobadoTrueAndActivoTrue();
    List<Restaurant> findByAprobadoFalse();

    Optional<Restaurant> findByUsuario_Email(String email);

}
