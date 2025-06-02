package com.example.Foodies.Restaurant;

import com.example.Foodies.Usuario.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.RestController;

@Repository
public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {

    boolean existByUsuarioId(Long usuarioId);

}
