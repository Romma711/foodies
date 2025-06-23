package com.example.Foodies.Restaurant;

import com.example.Foodies.Enums.EspecialidadDeComida;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {



    List<Restaurant> findByEspecialidad(EspecialidadDeComida especialidadDeComida);

    List<Restaurant> findByAprobadoTrue();

}
