package com.example.Foodies.Espacio;

import com.example.Foodies.Restaurant.Restaurant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Espacios de un restaurante: alta, edición, baja lógica y capacidad total derivada. */
@Service
public class EspacioService {

    private final EspacioRepository espacioRepo;

    public EspacioService(EspacioRepository espacioRepo) {
        this.espacioRepo = espacioRepo;
    }

    /** Crea el "Salón principal" de un restaurante recién registrado. */
    @Transactional
    public Espacio crearSalonPrincipal(Restaurant restaurant, int capacidad) {
        return espacioRepo.save(Espacio.salonPrincipal(restaurant, capacidad));
    }

    /**
     * Recalcula {@code cupoMaximo} como la suma de los espacios activos. Se mantiene como columna
     * porque admin, moderación y el panel lo muestran como capacidad total.
     */
    @Transactional
    public void recalcularCapacidadTotal(Restaurant restaurant) {
        espacioRepo.flush();
        restaurant.setCupoMaximo(espacioRepo.capacidadTotal(restaurant.getId()));
    }
}
