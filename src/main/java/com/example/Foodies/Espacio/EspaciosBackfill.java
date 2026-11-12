package com.example.Foodies.Espacio;

import com.example.Foodies.Restaurant.Restaurant;
import com.example.Foodies.Restaurant.RestaurantRepository;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Al arrancar, migra los restaurantes creados antes de que existieran los espacios.
 *
 * <p>A cada restaurante sin espacios le crea "Salón principal" con capacidad igual a su
 * {@code cupoMaximo}. Después asigna las reservas y los cupos especiales sin espacio al primer
 * espacio del restaurante. Es idempotente: solo toca restaurantes sin espacios y filas con
 * espacio {@code null}.</p>
 */
@Component
public class EspaciosBackfill implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(EspaciosBackfill.class);

    private final RestaurantRepository restaurantRepo;
    private final EspacioRepository espacioRepo;
    private final EntityManager em;

    public EspaciosBackfill(RestaurantRepository restaurantRepo, EspacioRepository espacioRepo, EntityManager em) {
        this.restaurantRepo = restaurantRepo;
        this.espacioRepo = espacioRepo;
        this.em = em;
    }

    /**
     * Ejecuta la migración. También se puede invocar a mano (por ejemplo desde un test).
     *
     * @param args argumentos de arranque; no se usan
     */
    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        int creados = 0;
        int asignadas = 0;
        for (Restaurant restaurant : restaurantRepo.findAll()) {
            if (!espacioRepo.existsByRestaurant_Id(restaurant.getId())) {
                int capacidad = restaurant.getCupoMaximo() == null ? 1 : Math.max(1, restaurant.getCupoMaximo());
                espacioRepo.save(Espacio.salonPrincipal(restaurant, capacidad));
                creados++;
            }
            Espacio principal = espacioRepo.findAllByRestaurant_IdOrderByIdAsc(restaurant.getId()).get(0);
            asignadas += asignar("Reserva", principal);
            asignadas += asignar("CupoEspecial", principal);
        }
        if (creados + asignadas > 0) {
            log.info("Espacios: {} salones principales creados, {} reservas/cupos asignados", creados, asignadas);
        }
    }

    private int asignar(String entidad, Espacio espacio) {
        return em.createQuery("update " + entidad + " x set x.espacio = :espacio "
                        + "where x.espacio is null and x.restaurant.id = :restaurantId")
                .setParameter("espacio", espacio)
                .setParameter("restaurantId", espacio.getRestaurant().getId())
                .executeUpdate();
    }
}
