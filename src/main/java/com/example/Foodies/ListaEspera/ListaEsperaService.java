package com.example.Foodies.ListaEspera;

import com.example.Foodies.Config.CurrentUser;
import com.example.Foodies.Enums.EstadoReserva;
import com.example.Foodies.Enums.Role;
import com.example.Foodies.Espacio.EspacioRepository;
import com.example.Foodies.Exception.BusinessException;
import com.example.Foodies.Exception.ConflictException;
import com.example.Foodies.Exception.EntityNotFoundException;
import com.example.Foodies.Exception.ForbiddenException;
import com.example.Foodies.Horario.HorarioAtencionService;
import com.example.Foodies.ListaEspera.Dtos.EntradaListaEsperaDTO;
import com.example.Foodies.ListaEspera.Dtos.ListaEsperaRequestDTO;
import com.example.Foodies.Moderacion.AvisosReserva;
import com.example.Foodies.Moderacion.ModeracionService;
import com.example.Foodies.Reserva.CupoReservas;
import com.example.Foodies.Reserva.Reserva;
import com.example.Foodies.Reserva.ReservaRepository;
import com.example.Foodies.Reserva.ReservaValidations;
import com.example.Foodies.Restaurant.Restaurant;
import com.example.Foodies.Restaurant.RestaurantRepository;
import com.example.Foodies.Turno.Turno;
import com.example.Foodies.Turno.TurnoService;
import com.example.Foodies.Usuario.Usuario;
import com.example.Foodies.Usuario.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Waitlist (RF25, D15): a client joins when the slot is full; when capacity frees up the first
 * waiting entry that fits is emailed and marked NOTIFICADA. The spot is not held: the client books
 * as usual. Entries expire once their date and time pass.
 */
@Service
public class ListaEsperaService {

    private static final Logger log = LoggerFactory.getLogger(ListaEsperaService.class);
    private static final Set<EstadoListaEspera> ABIERTAS = EnumSet.of(EstadoListaEspera.ESPERANDO,
            EstadoListaEspera.NOTIFICADA);
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final ListaEsperaRepository listaRepo;
    private final ReservaRepository reservaRepo;
    private final RestaurantRepository restaurantRepo;
    private final UsuarioRepository usuarioRepo;
    private final EspacioRepository espacioRepo;
    private final TurnoService turnoService;
    private final CupoReservas cupoReservas;
    private final HorarioAtencionService horarioService;
    private final AvisosReserva avisos;
    private final CurrentUser currentUser;
    private final Clock clock;

    public ListaEsperaService(ListaEsperaRepository listaRepo, ReservaRepository reservaRepo,
                              RestaurantRepository restaurantRepo, UsuarioRepository usuarioRepo,
                              EspacioRepository espacioRepo, TurnoService turnoService, CupoReservas cupoReservas,
                              HorarioAtencionService horarioService, AvisosReserva avisos,
                              CurrentUser currentUser, Clock clock) {
        this.listaRepo = listaRepo;
        this.reservaRepo = reservaRepo;
        this.restaurantRepo = restaurantRepo;
        this.usuarioRepo = usuarioRepo;
        this.espacioRepo = espacioRepo;
        this.turnoService = turnoService;
        this.cupoReservas = cupoReservas;
        this.horarioService = horarioService;
        this.avisos = avisos;
        this.currentUser = currentUser;
        this.clock = clock;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public EntradaListaEsperaDTO anotar(ListaEsperaRequestDTO pedido) {
        // Same lock order as a booking: the client, then the restaurant.
        Usuario usuario = usuarioRepo.bloquearPorEmail(currentUser.email())
                .orElseThrow(() -> new ForbiddenException("No hay un usuario autenticado"));
        if (usuario.getRol() != Role.ROLE_CLIENTE) {
            throw new ForbiddenException("Solo los clientes pueden anotarse en una lista de espera");
        }
        if (usuario.getStrikes() != null && usuario.getStrikes() >= ModeracionService.LIMITE_STRIKES) {
            throw new ForbiddenException(("Tu cuenta tiene %d strikes: no podés anotarte en listas de espera. "
                    + "Escribinos si creés que es un error.").formatted(ModeracionService.LIMITE_STRIKES));
        }
        if (pedido == null) {
            throw new BusinessException("No hay datos para anotarte");
        }
        if (pedido.cantidad() == null || pedido.cantidad() <= 0) {
            throw new BusinessException("La cantidad de personas debe ser mayor a 0");
        }
        if (pedido.idRestaurant() == null) {
            throw new BusinessException("El restaurante es obligatorio");
        }
        Restaurant restaurant = cupoReservas.bloquear(pedido.idRestaurant());
        if (!restaurant.isPublicado()) {
            throw new EntityNotFoundException("Restaurante no encontrado");
        }
        LocalDateTime ahora = LocalDateTime.now(clock);
        ReservaValidations.validateDate(pedido.fechaReserva(), ahora.toLocalDate());
        LocalDate fecha = LocalDate.parse(pedido.fechaReserva().trim());
        ReservaValidations.validateTime(pedido.horarioLlegada());
        LocalTime hora = LocalTime.parse(pedido.horarioLlegada().trim());
        Turno turno = turnoService.resolver(restaurant, fecha, pedido.idTurno(), hora).orElse(null);
        if (turno != null) {
            hora = turno.getInicio();
        } else {
            horarioService.validarLlegada(restaurant, fecha, hora);
        }
        ReservaValidations.validateNotPast(fecha, hora, ahora);

        if (!listaRepo.findAllByUsuario_IdAndRestaurant_IdAndFechaAndEstadoIn(usuario.getId(), restaurant.getId(),
                fecha, ABIERTAS).isEmpty()) {
            throw new ConflictException("Ya estás en la lista de espera de este restaurante ese día");
        }
        boolean yaReservo = reservaRepo.findAllByUsuario_IdAndEstadoReservaIn(usuario.getId(), EstadoReserva.ACTIVOS)
                .stream()
                .anyMatch(r -> r.getRestaurant() != null && r.getRestaurant().getId().equals(restaurant.getId())
                        && fecha.equals(r.fechaLocal()));
        if (yaReservo) {
            throw new ConflictException("Ya tenés una reserva activa en este restaurante ese día");
        }
        if (hayLugar(restaurant, fecha, turno, pedido.cantidad())) {
            throw new ConflictException("Hay lugar en ese horario: reservá directamente");
        }

        EntradaListaEspera entrada = new EntradaListaEspera();
        entrada.setUsuario(usuario);
        entrada.setRestaurant(restaurant);
        entrada.setFecha(fecha);
        entrada.setTurno(turno);
        entrada.setHorario(hora);
        entrada.setCantidad(pedido.cantidad());
        entrada.setEstado(EstadoListaEspera.ESPERANDO);
        entrada.setCreada(ahora);
        return toDto(listaRepo.save(entrada));
    }

    /** The client leaves the list. */
    @Transactional
    public void salir(Long entradaId) {
        EntradaListaEspera entrada = listaRepo.findById(entradaId)
                .orElseThrow(() -> new EntityNotFoundException("Inscripción no encontrada"));
        if (!entrada.getUsuario().getId().equals(usuarioActual().getId())) {
            throw new ForbiddenException("Solo podés salir de tus propias listas de espera");
        }
        if (!entrada.abierta()) {
            throw new BusinessException("Esa inscripción ya está cerrada");
        }
        entrada.setEstado(EstadoListaEspera.CANCELADA);
    }

    /** The authenticated client's entries, newest first, without the ones it left. */
    @Transactional(readOnly = true)
    public List<EntradaListaEsperaDTO> mias() {
        return listaRepo.findAllByUsuario_IdAndEstadoNotOrderByCreadaDesc(usuarioActual().getId(),
                        EstadoListaEspera.CANCELADA).stream()
                .map(ListaEsperaService::toDto)
                .toList();
    }

    /**
     * A reservation stopped holding capacity (cancelled or rejected): the first waiting entry of the
     * same day and shift that now fits is emailed. Nothing happens for an unpublished restaurant or a
     * slot that already passed. Call it after the reservation was saved, inside its transaction.
     */
    @Transactional
    public void lugarLiberado(Reserva liberada) {
        Restaurant restaurant = liberada.getRestaurant();
        LocalDate fecha = liberada.fechaLocal();
        if (restaurant == null || fecha == null || !restaurant.isPublicado()) {
            return;
        }
        reservaRepo.flush();
        LocalDateTime ahora = LocalDateTime.now(clock);
        Long turnoId = liberada.getTurno() == null ? null : liberada.getTurno().getId();
        listaRepo.findAllByRestaurant_IdAndFechaAndEstadoOrderByCreadaAscIdAsc(restaurant.getId(), fecha,
                        EstadoListaEspera.ESPERANDO).stream()
                .filter(e -> Objects.equals(e.getTurno() == null ? null : e.getTurno().getId(), turnoId))
                .filter(e -> LocalDateTime.of(e.getFecha(), e.getHorario()).isAfter(ahora))
                .filter(e -> hayLugar(restaurant, fecha, e.getTurno(), e.getCantidad()))
                .findFirst()
                .ifPresent(e -> {
                    e.setEstado(EstadoListaEspera.NOTIFICADA);
                    e.setNotificada(ahora);
                    avisos.lugarLibre(e.getUsuario(), restaurant, e.getFecha(), e.getHorario(), e.getCantidad());
                });
    }

    /** The client booked that restaurant and day: its open entry is closed. */
    @Transactional
    public void cerrarAlReservar(Usuario usuario, Restaurant restaurant, LocalDate fecha) {
        listaRepo.findAllByUsuario_IdAndRestaurant_IdAndFechaAndEstadoIn(usuario.getId(), restaurant.getId(), fecha,
                ABIERTAS).forEach(e -> e.setEstado(EstadoListaEspera.CANCELADA));
    }

    /**
     * Scheduled job: open entries whose date and time passed become VENCIDA.
     *
     * @return how many entries expired
     */
    @Scheduled(initialDelayString = "${foodies.lista-espera.vencimiento.demora-inicial-ms:60000}",
            fixedDelayString = "${foodies.lista-espera.vencimiento.cada-ms:300000}")
    @Transactional
    public int vencer() {
        LocalDateTime ahora = LocalDateTime.now(clock);
        List<EntradaListaEspera> vencidas = listaRepo.findAllByEstadoInAndFechaLessThanEqual(ABIERTAS,
                        ahora.toLocalDate()).stream()
                .filter(e -> !LocalDateTime.of(e.getFecha(), e.getHorario()).isAfter(ahora))
                .toList();
        vencidas.forEach(e -> e.setEstado(EstadoListaEspera.VENCIDA));
        if (!vencidas.isEmpty()) {
            log.info("Lista de espera: {} inscripciones vencidas", vencidas.size());
        }
        return vencidas.size();
    }

    private boolean hayLugar(Restaurant restaurant, LocalDate fecha, Turno turno, int cantidad) {
        return espacioRepo.findAllByRestaurant_IdAndActivoTrueOrderByIdAsc(restaurant.getId()).stream()
                .anyMatch(e -> cupoReservas.cupo(e, fecha) - cupoReservas.ocupadas(e, fecha, turno, null) >= cantidad);
    }

    private Usuario usuarioActual() {
        return usuarioRepo.findByEmail(currentUser.email())
                .orElseThrow(() -> new ForbiddenException("No hay un usuario autenticado"));
    }

    private static EntradaListaEsperaDTO toDto(EntradaListaEspera e) {
        return new EntradaListaEsperaDTO(e.getId(), e.getRestaurant().getId(), e.getRestaurant().getNombre(),
                e.getFecha(), e.getHorario().format(HORA), e.getTurno() == null ? null : e.getTurno().getId(),
                e.getCantidad(), e.getEstado(), e.getCreada());
    }
}
