package com.example.Foodies.ListaEspera.Dtos;

/**
 * Body to join a waitlist (RF25), same fields as a reservation.
 *
 * @param idRestaurant   restaurant
 * @param fechaReserva   yyyy-MM-dd
 * @param horarioLlegada HH:mm; with shifts, the shift start (or send idTurno)
 * @param idTurno        optional shift
 * @param cantidad       party size
 */
public record ListaEsperaRequestDTO(Long idRestaurant, String fechaReserva, String horarioLlegada, Long idTurno,
                                    Integer cantidad) {
}
