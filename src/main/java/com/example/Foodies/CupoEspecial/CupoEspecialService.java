package com.example.Foodies.CupoEspecial;

import com.example.Foodies.CupoEspecial.Dtos.CupoEspecialDTO;
import com.example.Foodies.CupoEspecial.Dtos.CupoEspecialRequestDTO;
import com.example.Foodies.Espacio.Espacio;
import com.example.Foodies.Espacio.EspacioService;
import com.example.Foodies.Exception.BusinessException;
import com.example.Foodies.Exception.EntityNotFoundException;
import com.example.Foodies.Restaurant.Restaurant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/** Cupos especiales: capacidad distinta de un espacio para un día puntual (por ejemplo, el patio cerrado por lluvia). */
@Service
public class CupoEspecialService {

    private static final int MAX_MOTIVO = 120;

    private final CupoEspecialRepository cupoRepo;
    private final EspacioService espacioService;

    public CupoEspecialService(CupoEspecialRepository cupoRepo, EspacioService espacioService) {
        this.cupoRepo = cupoRepo;
        this.espacioService = espacioService;
    }

    /** Próximos cupos especiales de todos los espacios del restaurante, por fecha y espacio. */
    @Transactional(readOnly = true)
    public List<CupoEspecialDTO> listarProximos(Restaurant restaurant) {
        return cupoRepo.findAllByRestaurant_IdAndFechaGreaterThanEqualOrderByFechaAsc(restaurant.getId(), LocalDate.now())
                .stream()
                .filter(cupo -> cupo.getEspacio() != null)
                .sorted(Comparator.comparing(CupoEspecial::getFecha).thenComparing(c -> c.getEspacio().getId()))
                .map(cupo -> toDto(cupo, espacioService.personasReservadas(cupo.getEspacio(), cupo.getFecha())))
                .toList();
    }

    @Transactional
    public CupoEspecialDTO guardar(Restaurant restaurant, Long espacioId, LocalDate fecha, CupoEspecialRequestDTO request) {
        Espacio espacio = espacioService.delRestaurante(restaurant, espacioId);
        if (request == null || request.cupo() == null) {
            throw new BusinessException("Indicá el cupo para ese día");
        }
        int cupo = request.cupo();
        if (fecha.isBefore(LocalDate.now())) {
            throw new BusinessException("No podés cambiar el cupo de un día que ya pasó");
        }
        if (cupo < 0 || cupo > espacio.getCapacidad()) {
            throw new BusinessException("El cupo especial debe estar entre 0 y la capacidad normal del espacio (%d)"
                    .formatted(espacio.getCapacidad()));
        }
        String motivo = request.motivo() == null || request.motivo().isBlank() ? null : request.motivo().strip();
        if (motivo != null && motivo.length() > MAX_MOTIVO) {
            throw new BusinessException("El motivo no puede tener más de %d caracteres".formatted(MAX_MOTIVO));
        }
        // Por ahora no se puede dejar afuera a reservas que ya existen.
        int reservadas = espacioService.personasReservadas(espacio, fecha);
        if (cupo < reservadas) {
            throw new BusinessException("Ya hay %d personas reservadas ese día. No podés bajar el cupo a menos de %d."
                    .formatted(reservadas, reservadas));
        }

        CupoEspecial cupoEspecial = cupoRepo.findByEspacio_IdAndFecha(espacio.getId(), fecha)
                .orElseGet(() -> {
                    CupoEspecial nuevo = new CupoEspecial();
                    nuevo.setRestaurant(restaurant);
                    nuevo.setEspacio(espacio);
                    nuevo.setFecha(fecha);
                    return nuevo;
                });
        cupoEspecial.setCupo(cupo);
        cupoEspecial.setMotivo(motivo);
        return toDto(cupoRepo.save(cupoEspecial), reservadas);
    }

    @Transactional
    public void eliminar(Restaurant restaurant, Long espacioId, LocalDate fecha) {
        Espacio espacio = espacioService.delRestaurante(restaurant, espacioId);
        CupoEspecial cupoEspecial = cupoRepo.findByEspacio_IdAndFecha(espacio.getId(), fecha)
                .orElseThrow(() -> new EntityNotFoundException("No hay un cupo especial para ese día en este espacio"));
        cupoRepo.delete(cupoEspecial);
    }

    private static CupoEspecialDTO toDto(CupoEspecial cupo, int reservadas) {
        return new CupoEspecialDTO(cupo.getFecha(), cupo.getCupo(), cupo.getMotivo(), reservadas,
                cupo.getEspacio().getId(), cupo.getEspacio().getNombre());
    }
}
