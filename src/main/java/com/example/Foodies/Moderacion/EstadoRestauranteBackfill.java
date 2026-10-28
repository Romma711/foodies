package com.example.Foodies.Moderacion;

import com.example.Foodies.Enums.EstadoRestaurante;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Al arrancar, completa {@code estado} en los restaurantes creados antes de la Fase 3.
 *
 * <p>{@code ddl-auto=update} agrega la columna vacía. El estado se deriva de {@code aprobado}
 * (true → APROBADO, false → PENDIENTE), así ningún restaurante pendiente queda aprobado por
 * accidente. Es idempotente: solo toca filas con estado {@code null}.</p>
 */
@Component
public class EstadoRestauranteBackfill implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(EstadoRestauranteBackfill.class);

    private final ModeracionRestaurantRepository repository;

    public EstadoRestauranteBackfill(ModeracionRestaurantRepository repository) {
        this.repository = repository;
    }

    /**
     * Ejecuta el completado. También se puede invocar a mano (por ejemplo desde un test).
     *
     * @param args argumentos de arranque; no se usan
     */
    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        int aprobados = repository.completarEstado(true, EstadoRestaurante.APROBADO);
        int pendientes = repository.completarEstado(false, EstadoRestaurante.PENDIENTE);
        if (aprobados + pendientes > 0) {
            log.info("Estado completado en {} restaurantes aprobados y {} pendientes", aprobados, pendientes);
        }
    }
}
