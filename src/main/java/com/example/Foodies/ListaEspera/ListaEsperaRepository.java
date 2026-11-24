package com.example.Foodies.ListaEspera;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface ListaEsperaRepository extends JpaRepository<EntradaListaEspera, Long> {

    List<EntradaListaEspera> findAllByUsuario_IdAndEstadoNotOrderByCreadaDesc(Long usuarioId, EstadoListaEspera estado);

    List<EntradaListaEspera> findAllByUsuario_IdAndRestaurant_IdAndFechaAndEstadoIn(
            Long usuarioId, Long restaurantId, LocalDate fecha, Collection<EstadoListaEspera> estados);

    List<EntradaListaEspera> findAllByRestaurant_IdAndFechaAndEstadoOrderByCreadaAscIdAsc(
            Long restaurantId, LocalDate fecha, EstadoListaEspera estado);

    List<EntradaListaEspera> findAllByEstadoInAndFechaLessThanEqual(Collection<EstadoListaEspera> estados, LocalDate hasta);
}
