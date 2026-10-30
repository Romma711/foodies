package com.example.Foodies.Foto;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface FotoRestauranteRepository extends JpaRepository<FotoRestaurante, Long> {

    List<FotoRestaurante> findAllByRestaurant_Id(Long restaurantId);

    List<FotoRestaurante> findAllByRestaurant_IdInAndTipo(Collection<Long> restaurantIds, TipoFoto tipo);
}
