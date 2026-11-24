package com.example.Foodies.Reserva;

import com.example.Foodies.Enums.EstadoReserva;
import com.example.Foodies.Espacio.EspacioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled job: a reservation still {@code PENDIENTE} when its arrival time passes is closed as
 * {@code VENCIDA}, so it stops holding capacity and can no longer be accepted or cancelled.
 */
@Component
public class VencimientoReservas {

    private static final Logger log = LoggerFactory.getLogger(VencimientoReservas.class);

    private final ReservaRepository reservaRepo;
    private final Clock clock;

    public VencimientoReservas(ReservaRepository reservaRepo, Clock clock) {
        this.reservaRepo = reservaRepo;
        this.clock = clock;
    }

    /**
     * Runs every few minutes (first run one minute after startup, after the start-up migrations).
     *
     * @return how many reservations were closed
     */
    @Scheduled(initialDelayString = "${foodies.reservas.vencimiento.demora-inicial-ms:60000}",
            fixedDelayString = "${foodies.reservas.vencimiento.cada-ms:300000}")
    @Transactional
    public int vencerPendientes() {
        LocalDateTime ahora = LocalDateTime.now(clock);
        List<Reserva> vencidas = reservaRepo.findAllByEstadoReservaAndFechaReservaLessThanEqual(
                        EstadoReserva.PENDIENTE, EspacioService.comoFechaReserva(LocalDate.from(ahora)))
                .stream()
                .filter(r -> r.llegada() != null && !r.llegada().isAfter(ahora))
                .toList();
        vencidas.forEach(r -> r.setEstadoReserva(EstadoReserva.VENCIDA));
        reservaRepo.saveAll(vencidas);
        if (!vencidas.isEmpty()) {
            log.info("Reservas: {} pendientes quedaron vencidas", vencidas.size());
        }
        return vencidas.size();
    }
}
