package com.example.Foodies.Horario;

import com.example.Foodies.Exception.BusinessException;
import com.example.Foodies.Exception.EntityNotFoundException;
import com.example.Foodies.Horario.Dtos.FranjaHorariaDTO;
import com.example.Foodies.Restaurant.Restaurant;
import com.example.Foodies.Restaurant.RestaurantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Horario de atención de cada restaurante y su regla para las reservas: la hora de llegada tiene que
 * caer en una franja de ese día. Un restaurante sin horario cargado no tiene restricción.
 */
@Service
public class HorarioAtencionService {

    public static final int MAX_FRANJAS_POR_DIA = 3;
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final Comparator<HorarioAtencion> ORDEN =
            Comparator.comparing(HorarioAtencion::getDia).thenComparing(HorarioAtencion::getApertura);

    private final HorarioAtencionRepository horarioRepo;
    private final RestaurantRepository restaurantRepo;

    public HorarioAtencionService(HorarioAtencionRepository horarioRepo, RestaurantRepository restaurantRepo) {
        this.horarioRepo = horarioRepo;
        this.restaurantRepo = restaurantRepo;
    }

    @Transactional(readOnly = true)
    public List<FranjaHorariaDTO> listar(Restaurant restaurant) {
        return franjas(restaurant.getId()).stream().map(HorarioAtencionService::toDto).toList();
    }

    /** Horario de un restaurante visible al público (aprobado y activo). */
    @Transactional(readOnly = true)
    public List<FranjaHorariaDTO> listarPublico(Long restaurantId) {
        Restaurant restaurant = restaurantRepo.findById(restaurantId)
                .filter(r -> r.isAprobado() && !Boolean.FALSE.equals(r.getActivo()))
                .orElseThrow(() -> new EntityNotFoundException("Restaurante no encontrado"));
        return listar(restaurant);
    }

    /** Reemplaza la semana completa. Una lista vacía quita el horario (sin restricción). */
    @Transactional
    public List<FranjaHorariaDTO> reemplazar(Restaurant restaurant, List<FranjaHorariaDTO> pedidas) {
        List<HorarioAtencion> nuevas = new ArrayList<>();
        for (FranjaHorariaDTO pedida : pedidas == null ? List.<FranjaHorariaDTO>of() : pedidas) {
            nuevas.add(validar(restaurant, pedida));
        }
        Map<java.time.DayOfWeek, List<HorarioAtencion>> porDia = nuevas.stream()
                .collect(Collectors.groupingBy(HorarioAtencion::getDia));
        porDia.forEach((dia, delDia) -> {
            if (delDia.size() > MAX_FRANJAS_POR_DIA) {
                throw new BusinessException("Cada día puede tener hasta %d franjas".formatted(MAX_FRANJAS_POR_DIA));
            }
            List<HorarioAtencion> ordenadas = delDia.stream().sorted(ORDEN).toList();
            for (int i = 1; i < ordenadas.size(); i++) {
                HorarioAtencion anterior = ordenadas.get(i - 1);
                if (anterior.getCierre().equals(LocalTime.MIDNIGHT)
                        || ordenadas.get(i).getApertura().isBefore(anterior.getCierre())) {
                    throw new BusinessException("Las franjas del mismo día se superponen");
                }
            }
        });
        horarioRepo.borrarDelRestaurante(restaurant.getId());
        horarioRepo.flush();
        horarioRepo.saveAll(nuevas);
        return nuevas.stream().sorted(ORDEN).map(HorarioAtencionService::toDto).toList();
    }

    /**
     * Valida que la llegada caiga en una franja de ese día. No hace nada si el restaurante no cargó
     * horario.
     *
     * @throws BusinessException con un mensaje para el cliente que dice cuándo atiende ese día
     */
    @Transactional(readOnly = true)
    public void validarLlegada(Restaurant restaurant, LocalDate fecha, LocalTime hora) {
        List<HorarioAtencion> franjas = franjas(restaurant.getId());
        if (franjas.isEmpty()) {
            return;
        }
        List<HorarioAtencion> delDia = franjas.stream().filter(f -> f.getDia() == fecha.getDayOfWeek()).toList();
        if (delDia.isEmpty()) {
            throw new BusinessException("Ese día el restaurante está cerrado. Elegí otra fecha.");
        }
        if (delDia.stream().noneMatch(f -> f.incluye(hora))) {
            String rangos = delDia.stream()
                    .map(f -> "de %s a %s".formatted(f.getApertura().format(HORA), f.getCierre().format(HORA)))
                    .collect(Collectors.joining(" y "));
            throw new BusinessException("Ese día el restaurante recibe reservas %s. Elegí otro horario.".formatted(rangos));
        }
    }

    private List<HorarioAtencion> franjas(Long restaurantId) {
        return horarioRepo.findAllByRestaurant_Id(restaurantId).stream().sorted(ORDEN).toList();
    }

    private static HorarioAtencion validar(Restaurant restaurant, FranjaHorariaDTO pedida) {
        if (pedida == null || pedida.dia() == null) {
            throw new BusinessException("Indicá el día de cada franja");
        }
        LocalTime apertura = hora(pedida.apertura());
        LocalTime cierre = hora(pedida.cierre());
        if (!cierre.equals(LocalTime.MIDNIGHT) && !cierre.isAfter(apertura)) {
            throw new BusinessException("El cierre tiene que ser después de la apertura (usá 00:00 para medianoche)");
        }
        HorarioAtencion franja = new HorarioAtencion();
        franja.setRestaurant(restaurant);
        franja.setDia(pedida.dia());
        franja.setApertura(apertura);
        franja.setCierre(cierre);
        return franja;
    }

    private static LocalTime hora(String texto) {
        try {
            return LocalTime.parse(texto == null ? "" : texto.strip(), HORA);
        } catch (DateTimeParseException e) {
            throw new BusinessException("Las horas tienen que tener el formato HH:mm, por ejemplo 20:30");
        }
    }

    private static FranjaHorariaDTO toDto(HorarioAtencion franja) {
        return new FranjaHorariaDTO(franja.getDia(), franja.getApertura().format(HORA), franja.getCierre().format(HORA));
    }
}
