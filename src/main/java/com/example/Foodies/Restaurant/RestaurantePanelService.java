package com.example.Foodies.Restaurant;

import com.example.Foodies.Carta.CartaService;
import com.example.Foodies.Carta.Dtos.CartaDTO;
import com.example.Foodies.Config.CurrentUser;
import com.example.Foodies.CupoEspecial.CupoEspecialService;
import com.example.Foodies.CupoEspecial.Dtos.CupoEspecialDTO;
import com.example.Foodies.CupoEspecial.Dtos.CupoEspecialRequestDTO;
import com.example.Foodies.Espacio.Dtos.EspacioDTO;
import com.example.Foodies.Espacio.Dtos.EspacioRequestDTO;
import com.example.Foodies.Espacio.EspacioService;
import com.example.Foodies.Foto.Dtos.FotoDTO;
import com.example.Foodies.Foto.FotoService;
import com.example.Foodies.Enums.EstadoReserva;
import com.example.Foodies.Exception.BusinessException;
import com.example.Foodies.Exception.EntityNotFoundException;
import com.example.Foodies.Exception.ForbiddenException;
import com.example.Foodies.Moderacion.AvisosReserva;
import com.example.Foodies.Reserva.CupoReservas;
import com.example.Foodies.Reserva.Reserva;
import com.example.Foodies.Reserva.ReservaRepository;
import com.example.Foodies.Reserva.dtos.ReservaPanelDTO;
import com.example.Foodies.Restaurant.Dtos.MiRestauranteDTO;
import com.example.Foodies.Restaurant.Dtos.RestaurantPatchDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;
import java.util.Comparator;
import java.util.List;

/** Everything a manager does in their panel, always scoped to the restaurant of the authenticated user. */
@Service
public class RestaurantePanelService {

    /** The manager marks attendance from the arrival time up to this many hours later. */
    public static final int HORAS_PARA_MARCAR_ASISTENCIA = 48;
    private static final Set<EstadoReserva> ESTADOS_DEL_RESTAURANTE = EnumSet.of(EstadoReserva.ACEPTADA,
            EstadoReserva.RECHAZADA, EstadoReserva.CANCELADA, EstadoReserva.ASISTIO, EstadoReserva.NO_ASISTIO);

    private final RestaurantRepository restaurantRepo;
    private final ReservaRepository reservaRepo;
    private final CartaService cartaService;
    private final CupoEspecialService cupoEspecialService;
    private final FotoService fotoService;
    private final EspacioService espacioService;
    private final CurrentUser currentUser;
    private final CupoReservas cupoReservas;
    private final Clock clock;
    private final AvisosReserva avisos;

    public RestaurantePanelService(RestaurantRepository restaurantRepo, ReservaRepository reservaRepo,
                                   CartaService cartaService, CupoEspecialService cupoEspecialService,
                                   FotoService fotoService, EspacioService espacioService,
                                   CurrentUser currentUser, CupoReservas cupoReservas, Clock clock,
                                   AvisosReserva avisos) {
        this.restaurantRepo = restaurantRepo;
        this.reservaRepo = reservaRepo;
        this.cartaService = cartaService;
        this.cupoEspecialService = cupoEspecialService;
        this.fotoService = fotoService;
        this.espacioService = espacioService;
        this.currentUser = currentUser;
        this.cupoReservas = cupoReservas;
        this.clock = clock;
        this.avisos = avisos;
    }

    @Transactional(readOnly = true)
    public MiRestauranteDTO getMiRestaurante() {
        return toDto(miRestaurante());
    }

    @Transactional
    public MiRestauranteDTO actualizar(RestaurantPatchDTO patch) {
        validar(patch);
        Restaurant restaurant = miRestaurante();
        if (patch.getNombre() != null) {
            restaurant.setNombre(patch.getNombre().strip());
        }
        if (patch.getUbicacion() != null) {
            restaurant.setUbicacion(patch.getUbicacion().strip());
        }
        if (patch.getEspecialidad() != null) {
            restaurant.setEspecialidad(patch.getEspecialidad());
        }
        if (patch.getPorcentajeAutoAceptacion() != null) {
            restaurant.setPorcentajeAutoAceptacion(patch.getPorcentajeAutoAceptacion());
        }
        return toDto(restaurantRepo.save(restaurant));
    }

    public CartaDTO guardarCarta(MultipartFile archivo) {
        return cartaService.guardar(miRestaurante(), archivo);
    }

    public void eliminarCarta() {
        cartaService.eliminar(miRestaurante());
    }

    @Transactional(readOnly = true)
    public List<ReservaPanelDTO> getReservas() {
        return reservaRepo.findAllByRestaurant_Id(miRestaurante().getId()).stream()
                .map(ReservaPanelDTO::from)
                .sorted(Comparator.comparing(ReservaPanelDTO::fecha, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(ReservaPanelDTO::horarioLlegada, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    /**
     * The manager moves a reservation of their restaurant through its lifecycle:
     * <ul>
     *   <li>before the arrival time: PENDIENTE to ACEPTADA (capacity checked again) or RECHAZADA;
     *       PENDIENTE or ACEPTADA to CANCELADA;</li>
     *   <li>from the arrival time up to 48 hours later: ACEPTADA to ASISTIO or NO_ASISTIO.</li>
     * </ul>
     * Final states never change.
     */
    @Transactional
    public ReservaPanelDTO cambiarEstadoReserva(Long reservaId, EstadoReserva estado) {
        if (estado == null || !ESTADOS_DEL_RESTAURANTE.contains(estado)) {
            throw new BusinessException("El estado debe ser ACEPTADA, RECHAZADA, CANCELADA, ASISTIO o NO_ASISTIO");
        }
        Reserva reserva = reservaRepo.findById(reservaId)
                .orElseThrow(() -> new EntityNotFoundException("Reserva no encontrada"));
        Restaurant restaurant = miRestaurante();
        if (reserva.getRestaurant() == null || !reserva.getRestaurant().getId().equals(restaurant.getId())) {
            throw new ForbiddenException("La reserva no pertenece a tu restaurante");
        }
        cupoReservas.bloquear(restaurant.getId());
        EstadoReserva actual = reserva.getEstadoReserva();
        if (actual.esFinal()) {
            throw new BusinessException("La reserva ya está %s y no se puede cambiar".formatted(descripcion(actual)));
        }
        LocalDateTime ahora = LocalDateTime.now(clock);
        LocalDateTime llegada = reserva.llegada();
        if (estado == EstadoReserva.ASISTIO || estado == EstadoReserva.NO_ASISTIO) {
            if (actual != EstadoReserva.ACEPTADA) {
                throw new BusinessException("Solo podés marcar la asistencia de una reserva aceptada");
            }
            if (ahora.isBefore(llegada)) {
                throw new BusinessException("La asistencia se marca desde la hora de llegada de la reserva");
            }
            if (ahora.isAfter(llegada.plusHours(HORAS_PARA_MARCAR_ASISTENCIA))) {
                throw new BusinessException("Pasaron más de %d horas de la reserva: ya no se puede marcar la asistencia"
                        .formatted(HORAS_PARA_MARCAR_ASISTENCIA));
            }
        } else {
            if (!ahora.isBefore(llegada)) {
                throw new BusinessException(
                        "La hora de la reserva ya pasó: solo podés marcar si el cliente asistió o no");
            }
            if (estado == EstadoReserva.ACEPTADA) {
                if (actual == EstadoReserva.ACEPTADA) {
                    throw new BusinessException("La reserva ya está aceptada");
                }
                if (reserva.getEspacio() != null) {
                    cupoReservas.validar(reserva.getEspacio(), reserva.fechaLocal(), reserva.getCantidad(),
                            reserva.getId());
                }
            }
            if (estado == EstadoReserva.RECHAZADA && actual != EstadoReserva.PENDIENTE) {
                throw new BusinessException("Solo podés rechazar una reserva pendiente; una aceptada se cancela");
            }
        }
        reserva.setEstadoReserva(estado);
        Reserva guardada = reservaRepo.save(reserva);
        if (estado == EstadoReserva.RECHAZADA) {
            avisos.rechazada(guardada);
        } else if (estado == EstadoReserva.CANCELADA) {
            avisos.canceladaPorRestaurante(guardada);
        }
        return ReservaPanelDTO.from(guardada);
    }

    /** How a final state reads in an error message, e.g. "La reserva ya está cancelada". */
    private static String descripcion(EstadoReserva estado) {
        return switch (estado) {
            case CANCELADA -> "cancelada";
            case RECHAZADA -> "rechazada";
            case ASISTIO -> "marcada como asistida";
            case NO_ASISTIO -> "marcada como ausente";
            case VENCIDA -> "vencida";
            default -> estado.name().toLowerCase();
        };
    }

    public List<EspacioDTO> getEspacios() {
        return espacioService.listar(miRestaurante());
    }

    public EspacioDTO crearEspacio(EspacioRequestDTO request) {
        return espacioService.crear(miRestaurante(), request);
    }

    public EspacioDTO actualizarEspacio(Long espacioId, EspacioRequestDTO request) {
        return espacioService.actualizar(miRestaurante(), espacioId, request);
    }

    public EspacioDTO cambiarActivoEspacio(Long espacioId, Boolean activo) {
        return espacioService.cambiarActivo(miRestaurante(), espacioId, activo);
    }

    public List<CupoEspecialDTO> getCuposEspeciales() {
        return cupoEspecialService.listarProximos(miRestaurante());
    }

    public CupoEspecialDTO guardarCupoEspecial(Long espacioId, LocalDate fecha, CupoEspecialRequestDTO request) {
        return cupoEspecialService.guardar(miRestaurante(), espacioId, fecha, request);
    }

    public void eliminarCupoEspecial(Long espacioId, LocalDate fecha) {
        cupoEspecialService.eliminar(miRestaurante(), espacioId, fecha);
    }

    public List<FotoDTO> getFotos() {
        return fotoService.listar(miRestaurante().getId());
    }

    public FotoDTO agregarFoto(MultipartFile archivo) {
        return fotoService.agregar(miRestaurante(), archivo);
    }

    public List<FotoDTO> elegirPortada(Long fotoId) {
        return fotoService.elegirPortada(miRestaurante(), fotoId);
    }

    public FotoDTO reemplazarFoto(Long fotoId, MultipartFile archivo) {
        return fotoService.reemplazar(miRestaurante(), fotoId, archivo);
    }

    public void eliminarFoto(Long fotoId) {
        fotoService.eliminar(miRestaurante(), fotoId);
    }

    private Restaurant miRestaurante() {
        return restaurantRepo.findByUsuario_Email(currentUser.email())
                .orElseThrow(() -> new EntityNotFoundException("No tenés un restaurante asociado"));
    }

    private MiRestauranteDTO toDto(Restaurant restaurant) {
        Integer porcentaje = restaurant.getPorcentajeAutoAceptacion();
        return new MiRestauranteDTO(restaurant.getId(), restaurant.getNombre(), restaurant.getCupoMaximo(),
                porcentaje == null ? 0 : porcentaje, restaurant.getUbicacion(), restaurant.getEspecialidad(),
                cartaService.buscarPorRestaurante(restaurant).orElse(null));
    }

    private static void validar(RestaurantPatchDTO patch) {
        if (patch == null) {
            throw new BusinessException("No hay datos para actualizar");
        }
        if (patch.getNombre() != null && patch.getNombre().isBlank()) {
            throw new BusinessException("El nombre no puede estar vacío");
        }
        if (patch.getUbicacion() != null && patch.getUbicacion().isBlank()) {
            throw new BusinessException("La ubicación no puede estar vacía");
        }
        if (patch.getCupoMaximo() != null) {
            throw new BusinessException(RestaurantPatchDTO.CAPACIDAD_POR_ESPACIOS);
        }
        Integer porcentaje = patch.getPorcentajeAutoAceptacion();
        if (porcentaje != null && (porcentaje < 0 || porcentaje > 100)) {
            throw new BusinessException("El porcentaje de aceptación automática debe estar entre 0 y 100");
        }
    }
}
