package com.example.Foodies.Turno;

import com.example.Foodies.Enums.EstadoReserva;
import com.example.Foodies.Espacio.Espacio;
import com.example.Foodies.Espacio.EspacioRepository;
import com.example.Foodies.Exception.BusinessException;
import com.example.Foodies.Exception.ConflictException;
import com.example.Foodies.Exception.EntityNotFoundException;
import com.example.Foodies.Horario.HorarioAtencionService;
import com.example.Foodies.Reserva.CupoReservas;
import com.example.Foodies.Reserva.ReservaRepository;
import com.example.Foodies.Restaurant.Restaurant;
import com.example.Foodies.Restaurant.RestaurantRepository;
import com.example.Foodies.Turno.Dtos.DisponibilidadDTO;
import com.example.Foodies.Turno.Dtos.EspacioDisponibleDTO;
import com.example.Foodies.Turno.Dtos.TurnoDTO;
import com.example.Foodies.Turno.Dtos.TurnoDisponibleDTO;
import com.example.Foodies.Turno.Dtos.TurnoRequestDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Shifts of a restaurant (RF24, D2): CRUD for the manager, public list and availability, and the rule
 * that picks the shift of a reservation.
 */
@Service
public class TurnoService {

    public static final int DURACION_MINIMA = 15;
    public static final int DURACION_MAXIMA = 360;
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final TurnoRepository turnoRepo;
    private final RestaurantRepository restaurantRepo;
    private final EspacioRepository espacioRepo;
    private final ReservaRepository reservaRepo;
    private final HorarioAtencionService horarioService;
    private final CupoReservas cupoReservas;
    private final Clock clock;

    public TurnoService(TurnoRepository turnoRepo, RestaurantRepository restaurantRepo, EspacioRepository espacioRepo,
                        ReservaRepository reservaRepo, HorarioAtencionService horarioService,
                        CupoReservas cupoReservas, Clock clock) {
        this.turnoRepo = turnoRepo;
        this.restaurantRepo = restaurantRepo;
        this.espacioRepo = espacioRepo;
        this.reservaRepo = reservaRepo;
        this.horarioService = horarioService;
        this.cupoReservas = cupoReservas;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<TurnoDTO> listar(Restaurant restaurant) {
        return turnoRepo.findAllByRestaurant_IdAndActivoTrueOrderByDiaAscInicioAsc(restaurant.getId()).stream()
                .map(TurnoDTO::from)
                .toList();
    }

    /** Shifts of a restaurant visible to the public. */
    @Transactional(readOnly = true)
    public List<TurnoDTO> listarPublico(Long restaurantId) {
        return listar(publicado(restaurantId));
    }

    @Transactional
    public TurnoDTO crear(Restaurant restaurant, TurnoRequestDTO request) {
        Turno turno = new Turno();
        turno.setRestaurant(restaurant);
        aplicar(restaurant, turno, request, null);
        return TurnoDTO.from(turnoRepo.save(turno));
    }

    @Transactional
    public TurnoDTO actualizar(Restaurant restaurant, Long turnoId, TurnoRequestDTO request) {
        Turno turno = delRestaurante(restaurant, turnoId);
        validarSinReservasFuturas(turno);
        aplicar(restaurant, turno, request, turnoId);
        return TurnoDTO.from(turnoRepo.save(turno));
    }

    /** Removes a shift from the restaurant; its past reservations keep pointing to it. */
    @Transactional
    public void eliminar(Restaurant restaurant, Long turnoId) {
        Turno turno = delRestaurante(restaurant, turnoId);
        validarSinReservasFuturas(turno);
        turno.setActivo(false);
        turnoRepo.save(turno);
    }

    /**
     * Shift of a reservation. Empty when the restaurant has no shifts (per-day capacity). Otherwise
     * the shift is {@code idTurno} or the one starting at {@code hora}, on that weekday.
     *
     * @throws BusinessException when the restaurant works with shifts and none matches
     */
    @Transactional(readOnly = true)
    public Optional<Turno> resolver(Restaurant restaurant, LocalDate fecha, Long idTurno, LocalTime hora) {
        if (!turnoRepo.existsByRestaurant_IdAndActivoTrue(restaurant.getId())) {
            if (idTurno != null) {
                throw new BusinessException("SIN_TURNOS", java.util.Map.of(), "Este restaurante no trabaja con turnos: elegí solo el horario de llegada");
            }
            return Optional.empty();
        }
        List<Turno> delDia = turnoRepo.findAllByRestaurant_IdAndDiaAndActivoTrueOrderByInicioAsc(
                restaurant.getId(), fecha.getDayOfWeek());
        if (delDia.isEmpty()) {
            throw new BusinessException("DIA_SIN_TURNOS", java.util.Map.of(), "Ese día el restaurante no tiene turnos. Elegí otra fecha.");
        }
        Optional<Turno> elegido = idTurno != null
                ? delDia.stream().filter(t -> t.getId().equals(idTurno)).findFirst()
                : delDia.stream().filter(t -> t.getInicio().equals(hora)).findFirst();
        if (elegido.isEmpty()) {
            String inicios = delDia.stream().map(t -> t.getInicio().format(HORA)).collect(Collectors.joining(", "));
            throw new BusinessException("TURNO_INVALIDO", java.util.Map.of("turnos", delDia.stream().map(t -> t.getInicio().format(HORA)).toList()), 
                    "Ese día el restaurante recibe reservas por turnos: %s. Elegí uno.".formatted(inicios));
        }
        Turno turno = elegido.get();
        horarioService.validarTurno(restaurant, turno.getDia(), turno.getInicio(), turno.finEnMinutos());
        return elegido;
    }

    /** Free seats per shift and active space on a date; per day when the restaurant has no shifts. */
    @Transactional(readOnly = true)
    public DisponibilidadDTO disponibilidad(Long restaurantId, LocalDate fecha) {
        if (fecha == null) {
            throw new BusinessException("FECHA_OBLIGATORIA", java.util.Map.of(), "Indicá la fecha");
        }
        Restaurant restaurant = publicado(restaurantId);
        List<Espacio> espacios = espacioRepo.findAllByRestaurant_IdAndActivoTrueOrderByIdAsc(restaurantId);
        if (!turnoRepo.existsByRestaurant_IdAndActivoTrue(restaurantId)) {
            return new DisponibilidadDTO(fecha, false,
                    List.of(new TurnoDisponibleDTO(null, null, null, libres(espacios, fecha, null))));
        }
        LocalDateTime ahora = LocalDateTime.now(clock);
        List<TurnoDisponibleDTO> turnos = turnoRepo
                .findAllByRestaurant_IdAndDiaAndActivoTrueOrderByInicioAsc(restaurant.getId(), fecha.getDayOfWeek())
                .stream()
                .filter(t -> LocalDateTime.of(fecha, t.getInicio()).isAfter(ahora))
                .map(t -> new TurnoDisponibleDTO(t.getId(), t.getInicio().format(HORA), t.fin().format(HORA),
                        libres(espacios, fecha, t)))
                .toList();
        return new DisponibilidadDTO(fecha, true, turnos);
    }

    private List<EspacioDisponibleDTO> libres(List<Espacio> espacios, LocalDate fecha, Turno turno) {
        return espacios.stream()
                .map(e -> new EspacioDisponibleDTO(e.getId(), e.getNombre(), e.getTipo(),
                        Math.max(0, cupoReservas.cupo(e, fecha) - cupoReservas.ocupadas(e, fecha, turno, null))))
                .toList();
    }

    private void aplicar(Restaurant restaurant, Turno turno, TurnoRequestDTO request, Long turnoId) {
        if (request == null || request.dia() == null) {
            throw new BusinessException("TURNO_SIN_DIA", java.util.Map.of(), "Indicá el día del turno");
        }
        LocalTime inicio;
        try {
            inicio = LocalTime.parse(request.inicio() == null ? "" : request.inicio().strip(), HORA);
        } catch (DateTimeParseException e) {
            throw new BusinessException("TURNO_HORA_INVALIDA", java.util.Map.of(), "La hora de inicio tiene que tener el formato HH:mm, por ejemplo 20:30");
        }
        int duracion = request.duracionMinutos() == null ? Turno.DURACION_POR_DEFECTO : request.duracionMinutos();
        if (duracion < DURACION_MINIMA || duracion > DURACION_MAXIMA) {
            throw new BusinessException("TURNO_DURACION_INVALIDA", java.util.Map.of("minimo", DURACION_MINIMA, "maximo", DURACION_MAXIMA), "La duración del turno tiene que estar entre %d y %d minutos"
                    .formatted(DURACION_MINIMA, DURACION_MAXIMA));
        }
        int fin = inicio.getHour() * 60 + inicio.getMinute() + duracion;
        if (fin > 24 * 60) {
            throw new BusinessException("TURNO_PASA_MEDIANOCHE", java.util.Map.of(), "El turno tiene que terminar a la medianoche como máximo");
        }
        boolean repetido = turnoRepo.findAllByRestaurant_IdAndDiaAndActivoTrueOrderByInicioAsc(restaurant.getId(),
                        request.dia()).stream()
                .anyMatch(t -> !t.getId().equals(turnoId) && t.getInicio().equals(inicio));
        if (repetido) {
            throw new ConflictException("TURNO_DUPLICADO", java.util.Map.of(), "Ya tenés un turno ese día a esa hora");
        }
        horarioService.validarTurno(restaurant, request.dia(), inicio, fin);
        turno.setDia(request.dia());
        turno.setInicio(inicio);
        turno.setDuracionMinutos(duracion);
        turno.setActivo(true);
    }

    private void validarSinReservasFuturas(Turno turno) {
        LocalDateTime ahora = LocalDateTime.now(clock);
        boolean conReservas = reservaRepo.findAllByTurno_IdAndEstadoReservaIn(turno.getId(), EstadoReserva.ACTIVOS)
                .stream()
                .anyMatch(r -> r.llegada() != null && r.llegada().isAfter(ahora));
        if (conReservas) {
            throw new ConflictException("TURNO_CON_RESERVAS", java.util.Map.of(), "El turno tiene reservas activas a futuro: no se puede cambiar ni borrar. "
                    + "Cancelalas primero.");
        }
    }

    private Turno delRestaurante(Restaurant restaurant, Long turnoId) {
        return turnoRepo.findById(turnoId)
                .filter(t -> t.getRestaurant().getId().equals(restaurant.getId()) && Boolean.TRUE.equals(t.getActivo()))
                .orElseThrow(() -> new EntityNotFoundException("TURNO_NO_ENCONTRADO", java.util.Map.of(), "Turno no encontrado"));
    }

    private Restaurant publicado(Long restaurantId) {
        return restaurantRepo.findById(restaurantId)
                .filter(Restaurant::isPublicado)
                .orElseThrow(() -> new EntityNotFoundException("RESTAURANTE_NO_ENCONTRADO", java.util.Map.of(), "Restaurante no encontrado"));
    }
}
