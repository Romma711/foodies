package com.example.Foodies.Reserva;

import com.example.Foodies.Config.CurrentUser;
import com.example.Foodies.CupoEspecial.CupoEspecialService;
import com.example.Foodies.Enums.EstadoReserva;
import com.example.Foodies.Exception.*;
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
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Date;
import java.util.List;
import java.util.function.Predicate;

@Service
public class ReservaService {

    @Autowired
    private ReservaRepository reservaRepo;

    @Autowired
    private UsuarioRepository usuarioRepo;

    @Autowired
    private RestaurantRepository restauranteRepo;
    @Autowired
    private CupoEspecialService cupoEspecialService;
    @Autowired
    private CurrentUser currentUser;

    public ReservaDetailDTO createReserva(ReservaRequesDTO reserva) {
        // The owner is always the authenticated user; a different idUsuario in the body is rejected.
        Usuario usuario = usuarioActual();
        if (reserva.getIdUsuario() != null && !reserva.getIdUsuario().equals(usuario.getId())) {
            throw new ForbiddenException("Solo podés crear reservas a tu nombre");
        }
        if (reserva.getCantidad() == null || reserva.getCantidad() <= 0) {
            throw new BusinessException("La cantidad de personas debe ser mayor a 0");
        }
        if (reserva.getIdRestaurant() == null) {
            throw new BusinessException("El restaurante es obligatorio");
        }

        Restaurant restaurante = restauranteRepo.findById(reserva.getIdRestaurant())
                .orElseThrow(() -> new EntityNotFoundException("Restaurante no encontrado"));

        //Esto verifica si el restaurant esta activo
        ReservaValidations.validateRestaurantIsActive(restaurante);

        ReservaValidations.validateTime(reserva.getHorarioLlegada());

        //Esto formatea de string a respectivos tipos de datos de tiempo (tanto la fecha, como la hora)
        LocalTime horaParseada = LocalTime.parse(reserva.getHorarioLlegada().trim());

        //Valida la fecha si es antes o despues de la fecha de hoy
        ReservaValidations.validateDate(reserva.getFechaReserva());

        Date fecha = parseFecha(reserva.getFechaReserva());
        //Aca valida si el cupo del dia no esta lleno, contando solo las reservas no canceladas de este restaurante
        //El cupo del dia es el cupo especial si el restaurante cargo uno, si no su capacidad normal
        int cupoDelDia = cupoEspecialService.cupoDelDia(restaurante, LocalDate.parse(reserva.getFechaReserva()));
        List<Reserva> reservasDelDia = reservaRepo.findAllByRestaurant_IdAndFechaReserva(restaurante.getId(), fecha);
        ReservaValidations.validateCupo(
                reserva.getCantidad(),
                personas(reservasDelDia, estado -> estado != EstadoReserva.CANCELADA),
                cupoDelDia);

        Reserva reservanueva = new Reserva();
        reservanueva.setCantidad(reserva.getCantidad());
        reservanueva.setFechaReserva(fecha);
        reservanueva.setHorariollegada(horaParseada);
        reservanueva.setEstadoReserva(estadoInicial(
                restaurante,
                cupoDelDia,
                reserva.getCantidad(),
                personas(reservasDelDia, estado -> estado == EstadoReserva.ACEPTADA)));
        reservanueva.setUsuario(usuario);
        reservanueva.setRestaurant(restaurante);
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

    private static int personas(List<Reserva> reservas, Predicate<EstadoReserva> estado) {
        return reservas.stream()
                .filter(r -> estado.test(r.getEstadoReserva()))
                .mapToInt(Reserva::getCantidad)
                .sum();
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
     * The owner changes date, arrival time and party size, or cancels the reservation.
     * A modified reservation goes back to {@code PENDIENTE} so the restaurant confirms it again.
     */
    @Transactional
    public ReservaDetailDTO updateReserva(Long id, ReservaPatchDTO dto) {
        Reserva r = buscarReserva(id);
        if (!esDuenio(r)) {
            throw new ForbiddenException("Solo podés modificar tus propias reservas");
        }
        if (dto == null) {
            throw new BusinessException("No hay datos para modificar");
        }
        if (r.getEstadoReserva() == EstadoReserva.CANCELADA) {
            throw new BusinessException("La reserva está cancelada y no se puede modificar");
        }
        if (dto.getEstadoReserva() != null) {
            if (dto.getEstadoReserva() != EstadoReserva.CANCELADA) {
                throw new BusinessException("Solo podés cancelar la reserva: el estado debe ser CANCELADA");
            }
            r.setEstadoReserva(EstadoReserva.CANCELADA);
            return toDetail(reservaRepo.save(r));
        }
        if (dto.getCantidad() == null && dto.getFechaReserva() == null && dto.getHorarioLlegada() == null) {
            throw new BusinessException("No hay datos para modificar");
        }

        int cantidad = dto.getCantidad() != null ? dto.getCantidad() : r.getCantidad();
        if (cantidad <= 0) {
            throw new BusinessException("La cantidad de personas debe ser mayor a 0");
        }
        String fechaTexto = dto.getFechaReserva() != null
                ? dto.getFechaReserva().trim()
                : new SimpleDateFormat("yyyy-MM-dd").format(r.getFechaReserva());
        ReservaValidations.validateDate(fechaTexto);
        LocalDate fecha = LocalDate.parse(fechaTexto);
        LocalTime hora = r.getHorariollegada();
        if (dto.getHorarioLlegada() != null) {
            ReservaValidations.validateTime(dto.getHorarioLlegada());
            hora = LocalTime.parse(dto.getHorarioLlegada().trim());
        }
        ReservaValidations.validateNotPast(fecha, hora);

        // Capacity of the new day, without counting this same reservation.
        Date fechaReserva = parseFecha(fechaTexto);
        int cupoDelDia = cupoEspecialService.cupoDelDia(r.getRestaurant(), fecha);
        List<Reserva> otrasDelDia = reservaRepo
                .findAllByRestaurant_IdAndFechaReserva(r.getRestaurant().getId(), fechaReserva).stream()
                .filter(otra -> !otra.getId().equals(r.getId()))
                .toList();
        ReservaValidations.validateCupo(
                cantidad,
                personas(otrasDelDia, estado -> estado != EstadoReserva.CANCELADA),
                cupoDelDia);

        r.setCantidad(cantidad);
        r.setFechaReserva(fechaReserva);
        r.setHorariollegada(hora);
        r.setEstadoReserva(EstadoReserva.PENDIENTE);
        return toDetail(reservaRepo.save(r));
    }

    /** The owner or an admin. */
    public void deleteReserva(Long id) {
        Reserva reserva = buscarReserva(id);
        if (!currentUser.isAdmin() && !esDuenio(reserva)) {
            throw new ForbiddenException("Solo podés borrar tus propias reservas");
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
                r.getRestaurant().getNombre()
        );
    }

    private static ReservaListDTO toList(Reserva r) {
        return new ReservaListDTO(
                r.getId(),
                r.getCantidad(),
                r.getFechaReserva(),
                r.getHorariollegada(),
                r.getEstadoReserva(),
                r.getRestaurant().getNombre()
        );
    }
}
