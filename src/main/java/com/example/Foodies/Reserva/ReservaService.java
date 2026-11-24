package com.example.Foodies.Reserva;

import com.example.Foodies.Config.CurrentUser;
import com.example.Foodies.Espacio.Espacio;
import com.example.Foodies.Espacio.EspacioService;
import com.example.Foodies.Horario.HorarioAtencionService;
import com.example.Foodies.Enums.EstadoReserva;
import com.example.Foodies.Enums.Role;
import com.example.Foodies.Exception.*;
import com.example.Foodies.Moderacion.ModeracionService;
import com.example.Foodies.Restaurant.Restaurant;
import com.example.Foodies.Restaurant.RestaurantRepository;
import com.example.Foodies.Reserva.dtos.*;
import com.example.Foodies.Usuario.Usuario;
import com.example.Foodies.Usuario.UsuarioRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.Exception;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;
import java.util.List;

@Service
public class ReservaService {

    /** The client cancels or modifies until this many minutes before arrival. */
    public static final int MINUTOS_LIMITE_CLIENTE = 30;
    /** Minimum gap between two active reservations of a client at different restaurants. */
    public static final int HORAS_ENTRE_RESERVAS = 2;

    @Autowired
    private ReservaRepository reservaRepo;

    @Autowired
    private UsuarioRepository usuarioRepo;

    @Autowired
    private RestaurantRepository restauranteRepo;
    @Autowired
    private EspacioService espacioService;
    @Autowired
    private HorarioAtencionService horarioService;
    @Autowired
    private CurrentUser currentUser;
    @Autowired
    private CupoReservas cupoReservas;
    @Autowired
    private Clock clock;

    @Transactional
    public ReservaDetailDTO createReserva(ReservaRequesDTO reserva) {
        // The owner is always the authenticated user; a different idUsuario in the body is rejected.
        Usuario usuario = usuarioActual();
        if (usuario.getRol() != Role.ROLE_CLIENTE) {
            throw new ForbiddenException("Solo los clientes pueden hacer reservas");
        }
        if (reserva.getIdUsuario() != null && !reserva.getIdUsuario().equals(usuario.getId())) {
            throw new ForbiddenException("Solo podés crear reservas a tu nombre");
        }
        validarSinBloqueoPorStrikes(usuario);
        if (reserva.getCantidad() == null || reserva.getCantidad() <= 0) {
            throw new BusinessException("La cantidad de personas debe ser mayor a 0");
        }
        if (reserva.getIdRestaurant() == null) {
            throw new BusinessException("El restaurante es obligatorio");
        }

        // Locked until commit: the capacity read below and the insert happen without another booking in between.
        Restaurant restaurante = cupoReservas.bloquear(reserva.getIdRestaurant());

        //Esto verifica si el restaurant esta activo
        ReservaValidations.validateRestaurantIsActive(restaurante);

        ReservaValidations.validateTime(reserva.getHorarioLlegada());

        //Esto formatea de string a respectivos tipos de datos de tiempo (tanto la fecha, como la hora)
        LocalTime horaParseada = LocalTime.parse(reserva.getHorarioLlegada().trim());

        //Valida la fecha si es antes o despues de la fecha de hoy
        ReservaValidations.validateDate(reserva.getFechaReserva(), LocalDate.now(clock));

        Date fecha = parseFecha(reserva.getFechaReserva());
        LocalDate dia = LocalDate.parse(reserva.getFechaReserva().trim());
        ReservaValidations.validateNotPast(dia, horaParseada, LocalDateTime.now(clock));
        horarioService.validarLlegada(restaurante, dia, horaParseada);
        validarSinDuplicados(usuario, restaurante, dia, horaParseada, null);
        // El cupo se valida en el espacio elegido, contando solo sus reservas activas.
        // El cupo del día es el cupo especial del espacio si lo tiene, si no su capacidad.
        Espacio espacio = espacioService.paraReservar(restaurante, reserva.getIdEspacio());
        cupoReservas.validar(espacio, dia, reserva.getCantidad(), null);

        Reserva reservanueva = new Reserva();
        reservanueva.setCantidad(reserva.getCantidad());
        reservanueva.setFechaReserva(fecha);
        reservanueva.setHorariollegada(horaParseada);
        reservanueva.setEstadoReserva(estadoInicial(
                restaurante,
                cupoReservas.cupo(espacio, dia),
                reserva.getCantidad(),
                cupoReservas.aceptadas(espacio, dia, null)));
        reservanueva.setUsuario(usuario);
        reservanueva.setRestaurant(restaurante);
        reservanueva.setEspacio(espacio);
        reservanueva = reservaRepo.save(reservanueva);

        return toDetail(reservanueva);
    }

    private static Date parseFecha(String fecha) {
        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        try {
            return formatter.parse(fecha);
        }catch (ParseException e) {
            throw new BusinessException("Fecha no valida, Tiene que ser yyyy-MM-dd. Ejemplo: 2022-01-01.");
        }catch (Exception e) {
            // Captura cualquier otra excepción relacionada con fechas
            throw new BusinessException("Error al procesar la fecha. Verifique el formato yyyy-MM-dd");
        }
    }

    /**
     * Se acepta sola si el restaurante tiene auto-aceptación y, sumando esta reserva, las personas aceptadas del día
     * no superan el porcentaje elegido del cupo del día. La reserva que cruza el límite queda pendiente.
     */
    private static EstadoReserva estadoInicial(Restaurant restaurante, int cupoDelDia, int cantidad,
                                               int aceptadasDelDia) {
        Integer porcentaje = restaurante.getPorcentajeAutoAceptacion();
        if (porcentaje == null || porcentaje <= 0) {
            return EstadoReserva.PENDIENTE;
        }
        int limite = cupoDelDia * porcentaje / 100;
        return aceptadasDelDia + cantidad <= limite ? EstadoReserva.ACEPTADA : EstadoReserva.PENDIENTE;
    }

    /** Every reservation of the platform. The URL rule limits it to {@code ROLE_ADMIN}. */
    public List<ReservaListDTO> getAllReservas() {
        return reservaRepo.findAll().stream().map(ReservaService::toList).toList();
    }

    /** The owner, the manager of the reservation's restaurant or an admin. */
    public ReservaDetailDTO getReservaById(Long id) {
        Reserva r = buscarReserva(id);
        if (!currentUser.isAdmin() && !esDuenio(r) && !esEncargadoDe(r.getRestaurant())) {
            throw new ForbiddenException("No podés ver una reserva de otro usuario");
        }
        return toDetail(r);
    }

    /**
     * The owner changes date, arrival time and party size, or cancels the reservation, until
     * {@value #MINUTOS_LIMITE_CLIENTE} minutes before arrival. A modified reservation is accepted again
     * only if auto-acceptance covers it; otherwise it goes back to {@code PENDIENTE}.
     */
    @Transactional
    public ReservaDetailDTO updateReserva(Long id, ReservaPatchDTO dto) {
        Reserva r = buscarReserva(id);
        if (!esDuenio(r)) {
            throw new ForbiddenException("Solo podés modificar tus propias reservas");
        }
        cupoReservas.bloquear(r.getRestaurant().getId());
        if (dto == null) {
            throw new BusinessException("No hay datos para modificar");
        }
        if (r.getEstadoReserva() == EstadoReserva.CANCELADA) {
            throw new BusinessException("La reserva está cancelada y no se puede modificar");
        }
        if (r.getEstadoReserva().esFinal()) {
            throw new BusinessException("La reserva ya está cerrada y no se puede modificar");
        }
        if (LocalDateTime.now(clock).isAfter(r.llegada().minusMinutes(MINUTOS_LIMITE_CLIENTE))) {
            throw new BusinessException(("Solo podés cancelar o modificar la reserva hasta %d minutos antes de la "
                    + "llegada. Para cambios de último momento, comunicate con el restaurante.")
                    .formatted(MINUTOS_LIMITE_CLIENTE));
        }
        if (dto.getEstadoReserva() != null) {
            if (dto.getEstadoReserva() != EstadoReserva.CANCELADA) {
                throw new BusinessException("Solo podés cancelar la reserva: el estado debe ser CANCELADA");
            }
            r.setEstadoReserva(EstadoReserva.CANCELADA);
            return toDetail(reservaRepo.save(r));
        }
        if (dto.getCantidad() == null && dto.getFechaReserva() == null && dto.getHorarioLlegada() == null
                && dto.getIdEspacio() == null) {
            throw new BusinessException("No hay datos para modificar");
        }

        int cantidad = dto.getCantidad() != null ? dto.getCantidad() : r.getCantidad();
        if (cantidad <= 0) {
            throw new BusinessException("La cantidad de personas debe ser mayor a 0");
        }
        String fechaTexto = dto.getFechaReserva() != null
                ? dto.getFechaReserva().trim()
                : new SimpleDateFormat("yyyy-MM-dd").format(r.getFechaReserva());
        ReservaValidations.validateDate(fechaTexto, LocalDate.now(clock));
        LocalDate fecha = LocalDate.parse(fechaTexto);
        LocalTime hora = r.getHorariollegada();
        if (dto.getHorarioLlegada() != null) {
            ReservaValidations.validateTime(dto.getHorarioLlegada());
            hora = LocalTime.parse(dto.getHorarioLlegada().trim());
        }
        ReservaValidations.validateNotPast(fecha, hora, LocalDateTime.now(clock));
        horarioService.validarLlegada(r.getRestaurant(), fecha, hora);
        validarSinDuplicados(r.getUsuario(), r.getRestaurant(), fecha, hora, r.getId());

        // Capacity of the (new) espacio on the new day, without counting this same reservation.
        Espacio espacio = dto.getIdEspacio() != null
                ? espacioService.paraReservar(r.getRestaurant(), dto.getIdEspacio())
                : r.getEspacio();
        if (espacio == null) {
            throw new BusinessException("Elegí en qué espacio querés reservar");
        }
        Date fechaReserva = parseFecha(fechaTexto);
        cupoReservas.validar(espacio, fecha, cantidad, r.getId());

        r.setCantidad(cantidad);
        r.setFechaReserva(fechaReserva);
        r.setHorariollegada(hora);
        r.setEspacio(espacio);
        // Modifying re-evaluates auto-acceptance; otherwise the restaurant confirms it again.
        r.setEstadoReserva(estadoInicial(r.getRestaurant(), cupoReservas.cupo(espacio, fecha), cantidad,
                cupoReservas.aceptadas(espacio, fecha, r.getId())));
        return toDetail(reservaRepo.save(r));
    }

    /** Admin only: clients cancel, so the restaurant and the history keep the reservation. */
    public void deleteReserva(Long id) {
        buscarReserva(id);
        if (!currentUser.isAdmin()) {
            throw new ForbiddenException("Las reservas no se borran: cancelala para liberar el lugar");
        }
        reservaRepo.deleteById(id);
    }

    /** The manager of that restaurant or an admin. */
    public List<ReservaListDTO> getAllByRestaurant(Long id) {
        if (!currentUser.isAdmin()) {
            Restaurant restaurant = restauranteRepo.findById(id)
                    .orElseThrow(() -> new EntityNotFoundException("Restaurante no encontrado"));
            if (!esEncargadoDe(restaurant)) {
                throw new ForbiddenException("No podés ver las reservas de otro restaurante");
            }
        }
        List<ReservaListDTO> reserva = reservaRepo.findAllByRestaurant_Id(id).stream()
                .map(ReservaService::toList).toList();
        if (reserva.isEmpty()) {
            throw new ListNoContentException("Este restaurante no tiene reservas");
        }
        return reserva;
    }

    /** The user themself or an admin. */
    public List<ReservaListDTO> getAllByUsuario(Long id) {
        if (!currentUser.isAdmin() && !id.equals(usuarioActual().getId())) {
            throw new ForbiddenException("No podés ver las reservas de otro usuario");
        }
        List<ReservaListDTO> resevas = reservaRepo.findAllByUsuario_Id(id).stream()
                .map(ReservaService::toList).toList();
        if(resevas.isEmpty()){
            throw  new ListNoContentException("Este usuario no tiene reservas");
        }
        return resevas;
    }

    /** With {@value ModeracionService#LIMITE_STRIKES} strikes the client keeps its account but cannot book. */
    public static void validarSinBloqueoPorStrikes(Usuario usuario) {
        if (usuario.getStrikes() != null && usuario.getStrikes() >= ModeracionService.LIMITE_STRIKES) {
            throw new ForbiddenException(("Tu cuenta tiene %d strikes: no podés hacer reservas nuevas. "
                    + "Escribinos si creés que es un error.").formatted(ModeracionService.LIMITE_STRIKES));
        }
    }

    /**
     * A client holds at most one active reservation per restaurant and day, and two active reservations
     * at different restaurants start at least {@value #HORAS_ENTRE_RESERVAS} hours apart.
     */
    public void validarSinDuplicados(Usuario usuario, Restaurant restaurante, LocalDate fecha, LocalTime hora,
                                     Long excluirId) {
        List<Reserva> activas = reservaRepo.findAllByUsuario_IdAndEstadoReservaIn(usuario.getId(), EstadoReserva.ACTIVOS)
                .stream()
                .filter(otra -> excluirId == null || !excluirId.equals(otra.getId()))
                .toList();
        boolean mismoRestaurante = activas.stream().anyMatch(otra -> otra.getRestaurant() != null
                && otra.getRestaurant().getId().equals(restaurante.getId()) && fecha.equals(otra.fechaLocal()));
        if (mismoRestaurante) {
            throw new ConflictException(
                    "Ya tenés una reserva activa en este restaurante ese día. Modificala si necesitás cambiarla.");
        }
        LocalDateTime llegada = LocalDateTime.of(fecha, hora);
        boolean cerca = activas.stream()
                .map(Reserva::llegada)
                .anyMatch(otra -> otra != null
                        && Math.abs(Duration.between(otra, llegada).toMinutes()) < HORAS_ENTRE_RESERVAS * 60L);
        if (cerca) {
            throw new ConflictException("Ya tenés otra reserva activa a menos de %d horas de este horario."
                    .formatted(HORAS_ENTRE_RESERVAS));
        }
    }

    private Reserva buscarReserva(Long id) {
        return reservaRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Reserva no encontrada"));
    }

    private Usuario usuarioActual() {
        return usuarioRepo.findByEmail(currentUser.email())
                .orElseThrow(() -> new ForbiddenException("No hay un usuario autenticado"));
    }

    private boolean esDuenio(Reserva reserva) {
        Usuario duenio = reserva.getUsuario();
        return duenio != null && duenio.getEmail() != null && duenio.getEmail().equalsIgnoreCase(currentUser.email());
    }

    private boolean esEncargadoDe(Restaurant restaurant) {
        Usuario encargado = restaurant == null ? null : restaurant.getUsuario();
        return encargado != null && encargado.getEmail() != null
                && encargado.getEmail().equalsIgnoreCase(currentUser.email());
    }

    private static ReservaDetailDTO toDetail(Reserva r) {
        return new ReservaDetailDTO(
                r.getId(),
                r.getCantidad(),
                r.getFechaReserva(),
                r.getHorariollegada(),
                r.getEstadoReserva(),
                r.getUsuario().getNombre(),
                r.getRestaurant().getNombre(),
                r.getEspacio() == null ? null : r.getEspacio().getId(),
                r.getEspacio() == null ? null : r.getEspacio().getNombre()
        );
    }

    private static ReservaListDTO toList(Reserva r) {
        return new ReservaListDTO(
                r.getId(),
                r.getCantidad(),
                r.getFechaReserva(),
                r.getHorariollegada(),
                r.getEstadoReserva(),
                r.getRestaurant().getNombre(),
                r.getRestaurant().getId(),
                r.getEspacio() == null ? null : r.getEspacio().getId(),
                r.getEspacio() == null ? null : r.getEspacio().getNombre()
        );
    }
}
