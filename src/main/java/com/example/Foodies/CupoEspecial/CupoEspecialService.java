package com.example.Foodies.CupoEspecial;

import com.example.Foodies.CupoEspecial.Dtos.CupoEspecialDTO;
import com.example.Foodies.CupoEspecial.Dtos.CupoEspecialRequestDTO;
import com.example.Foodies.Enums.EstadoReserva;
import com.example.Foodies.Exception.BusinessException;
import com.example.Foodies.Exception.EntityNotFoundException;
import com.example.Foodies.Reserva.Reserva;
import com.example.Foodies.Reserva.ReservaRepository;
import com.example.Foodies.Restaurant.Restaurant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

@Service
public class CupoEspecialService {

    private static final int MAX_MOTIVO = 120;

    private final CupoEspecialRepository cupoRepo;
    private final ReservaRepository reservaRepo;

    public CupoEspecialService(CupoEspecialRepository cupoRepo, ReservaRepository reservaRepo) {
        this.cupoRepo = cupoRepo;
        this.reservaRepo = reservaRepo;
    }

    /** Cupo que rige ese día: el especial si existe, si no la capacidad normal del restaurante. */
    @Transactional(readOnly = true)
    public int cupoDelDia(Restaurant restaurant, LocalDate fecha) {
        return cupoRepo.findByRestaurant_IdAndFecha(restaurant.getId(), fecha)
                .map(CupoEspecial::getCupo)
                .orElse(restaurant.getCupoMaximo());
    }

    @Transactional(readOnly = true)
    public List<CupoEspecialDTO> listarProximos(Restaurant restaurant) {
        return cupoRepo.findAllByRestaurant_IdAndFechaGreaterThanEqualOrderByFechaAsc(restaurant.getId(), LocalDate.now())
                .stream()
                .map(cupo -> toDto(cupo, personasReservadas(restaurant, cupo.getFecha())))
                .toList();
    }

    @Transactional
    public CupoEspecialDTO guardar(Restaurant restaurant, LocalDate fecha, CupoEspecialRequestDTO request) {
        if (request == null || request.cupo() == null) {
            throw new BusinessException("Indicá el cupo para ese día");
        }
        int cupo = request.cupo();
        if (fecha.isBefore(LocalDate.now())) {
            throw new BusinessException("No podés cambiar el cupo de un día que ya pasó");
        }
        if (cupo < 0 || cupo > restaurant.getCupoMaximo()) {
            throw new BusinessException("El cupo especial debe estar entre 0 y tu capacidad normal (%d)"
                    .formatted(restaurant.getCupoMaximo()));
        }
        String motivo = request.motivo() == null || request.motivo().isBlank() ? null : request.motivo().strip();
        if (motivo != null && motivo.length() > MAX_MOTIVO) {
            throw new BusinessException("El motivo no puede tener más de %d caracteres".formatted(MAX_MOTIVO));
        }
        // Por ahora no se puede dejar afuera a reservas que ya existen.
        int reservadas = personasReservadas(restaurant, fecha);
        if (cupo < reservadas) {
            throw new BusinessException("Ya hay %d personas reservadas ese día. No podés bajar el cupo a menos de %d."
                    .formatted(reservadas, reservadas));
        }

        CupoEspecial cupoEspecial = cupoRepo.findByRestaurant_IdAndFecha(restaurant.getId(), fecha)
                .orElseGet(() -> {
                    CupoEspecial nuevo = new CupoEspecial();
                    nuevo.setRestaurant(restaurant);
                    nuevo.setFecha(fecha);
                    return nuevo;
                });
        cupoEspecial.setCupo(cupo);
        cupoEspecial.setMotivo(motivo);
        return toDto(cupoRepo.save(cupoEspecial), reservadas);
    }

    @Transactional
    public void eliminar(Restaurant restaurant, LocalDate fecha) {
        CupoEspecial cupoEspecial = cupoRepo.findByRestaurant_IdAndFecha(restaurant.getId(), fecha)
                .orElseThrow(() -> new EntityNotFoundException("No hay un cupo especial para ese día"));
        cupoRepo.delete(cupoEspecial);
    }

    /** Personas con reservas no canceladas en ese restaurante ese día. */
    private int personasReservadas(Restaurant restaurant, LocalDate fecha) {
        // `fechaReserva` se guarda como medianoche en la zona horaria del servidor.
        Date dia = Date.from(fecha.atStartOfDay(ZoneId.systemDefault()).toInstant());
        return reservaRepo.findAllByRestaurant_IdAndFechaReserva(restaurant.getId(), dia).stream()
                .filter(r -> r.getEstadoReserva() != EstadoReserva.CANCELADA)
                .mapToInt(Reserva::getCantidad)
                .sum();
    }

    private static CupoEspecialDTO toDto(CupoEspecial cupo, int reservadas) {
        return new CupoEspecialDTO(cupo.getFecha(), cupo.getCupo(), cupo.getMotivo(), reservadas);
    }
}
