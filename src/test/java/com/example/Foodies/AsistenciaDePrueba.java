package com.example.Foodies;

import com.example.Foodies.Enums.EstadoReserva;
import com.example.Foodies.Espacio.EspacioService;
import com.example.Foodies.Reserva.Reserva;
import com.example.Foodies.Reserva.ReservaRepository;
import com.example.Foodies.Restaurant.RestaurantRepository;
import com.example.Foodies.Usuario.UsuarioRepository;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Test setup for reviews (D12): stores a past reservation the restaurant already marked as ASISTIO,
 * so a test about something else can review without walking the whole attendance flow.
 */
final class AsistenciaDePrueba {

    private AsistenciaDePrueba() {
    }

    static void registrar(ReservaRepository reservaRepo, UsuarioRepository usuarioRepo,
                          RestaurantRepository restaurantRepo, Long clienteId, Long restaurantId) {
        Reserva reserva = new Reserva();
        reserva.setCantidad(2);
        reserva.setFechaReserva(EspacioService.comoFechaReserva(LocalDate.now().minusDays(1)));
        reserva.setHorariollegada(LocalTime.of(20, 0));
        reserva.setEstadoReserva(EstadoReserva.ASISTIO);
        reserva.setUsuario(usuarioRepo.findById(clienteId).orElseThrow());
        reserva.setRestaurant(restaurantRepo.findById(restaurantId).orElseThrow());
        reservaRepo.save(reserva);
    }
}
