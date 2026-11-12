package com.example.Foodies.Espacio;

import com.example.Foodies.CupoEspecial.CupoEspecial;
import com.example.Foodies.CupoEspecial.CupoEspecialRepository;
import com.example.Foodies.Enums.EstadoReserva;
import com.example.Foodies.Espacio.Dtos.EspacioDTO;
import com.example.Foodies.Espacio.Dtos.EspacioPublicoDTO;
import com.example.Foodies.Espacio.Dtos.EspacioRequestDTO;
import com.example.Foodies.Exception.BusinessException;
import com.example.Foodies.Exception.EntityNotFoundException;
import com.example.Foodies.Reserva.Reserva;
import com.example.Foodies.Reserva.ReservaRepository;
import com.example.Foodies.Restaurant.Restaurant;
import com.example.Foodies.Restaurant.RestaurantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Espacios de un restaurante: alta, edición, baja lógica, cupo por día y capacidad total derivada. */
@Service
public class EspacioService {

    private final EspacioRepository espacioRepo;
    private final ReservaRepository reservaRepo;
    private final CupoEspecialRepository cupoRepo;
    private final RestaurantRepository restaurantRepo;

    public EspacioService(EspacioRepository espacioRepo, ReservaRepository reservaRepo,
                          CupoEspecialRepository cupoRepo, RestaurantRepository restaurantRepo) {
        this.espacioRepo = espacioRepo;
        this.reservaRepo = reservaRepo;
        this.cupoRepo = cupoRepo;
        this.restaurantRepo = restaurantRepo;
    }

    /** Crea el "Salón principal" de un restaurante recién registrado. */
    @Transactional
    public Espacio crearSalonPrincipal(Restaurant restaurant, int capacidad) {
        return espacioRepo.save(Espacio.salonPrincipal(restaurant, capacidad));
    }

    @Transactional(readOnly = true)
    public List<EspacioDTO> listar(Restaurant restaurant) {
        return espacioRepo.findAllByRestaurant_IdOrderByIdAsc(restaurant.getId()).stream()
                .map(EspacioDTO::from)
                .toList();
    }

    @Transactional
    public EspacioDTO crear(Restaurant restaurant, EspacioRequestDTO request) {
        String nombre = validar(restaurant, request, null);
        Espacio espacio = new Espacio();
        espacio.setRestaurant(restaurant);
        espacio.setTipo(request.tipo());
        espacio.setNombre(nombre);
        espacio.setCapacidad(request.capacidad());
        espacio.setActivo(true);
        espacio = espacioRepo.save(espacio);
        recalcularCapacidadTotal(restaurant);
        return EspacioDTO.from(espacio);
    }

    @Transactional
    public EspacioDTO actualizar(Restaurant restaurant, Long espacioId, EspacioRequestDTO request) {
        Espacio espacio = delRestaurante(restaurant, espacioId);
        String nombre = validar(restaurant, request, espacioId);
        int reservadas = maximoReservadoFuturo(espacio);
        if (request.capacidad() < reservadas) {
            throw new BusinessException(("No podés bajar la capacidad a %d: ya hay un día con %d personas "
                    + "reservadas en este espacio").formatted(request.capacidad(), reservadas));
        }
        espacio.setTipo(request.tipo());
        espacio.setNombre(nombre);
        espacio.setCapacidad(request.capacidad());
        espacio = espacioRepo.save(espacio);
        recalcularCapacidadTotal(restaurant);
        return EspacioDTO.from(espacio);
    }

    /**
     * Activa o desactiva un espacio. Un espacio con reservas futuras no se puede desactivar, y el
     * restaurante siempre conserva al menos un espacio activo.
     */
    @Transactional
    public EspacioDTO cambiarActivo(Restaurant restaurant, Long espacioId, Boolean activo) {
        if (activo == null) {
            throw new BusinessException("Indicá si el espacio queda activo");
        }
        Espacio espacio = delRestaurante(restaurant, espacioId);
        if (!activo && Boolean.TRUE.equals(espacio.getActivo())) {
            if (espacioRepo.findAllByRestaurant_IdAndActivoTrueOrderByIdAsc(restaurant.getId()).size() <= 1) {
                throw new BusinessException("Tu restaurante necesita al menos un espacio activo");
            }
            if (maximoReservadoFuturo(espacio) > 0) {
                throw new BusinessException("No podés desactivar un espacio con reservas pendientes o aceptadas "
                        + "para los próximos días. Cancelalas o movelas a otro espacio primero.");
            }
        }
        espacio.setActivo(activo);
        espacio = espacioRepo.save(espacio);
        recalcularCapacidadTotal(restaurant);
        return EspacioDTO.from(espacio);
    }

    /** Espacios activos de un restaurante público; con fecha y cantidad indica si hay lugar en cada uno. */
    @Transactional(readOnly = true)
    public List<EspacioPublicoDTO> listarPublicos(Long restaurantId, LocalDate fecha, Integer cantidad) {
        if ((fecha == null) != (cantidad == null)) {
            throw new BusinessException("Indicá la fecha y la cantidad de personas juntas");
        }
        if (cantidad != null && cantidad < 1) {
            throw new BusinessException("La cantidad de personas debe ser al menos 1");
        }
        restaurantRepo.findById(restaurantId)
                .filter(r -> r.isAprobado() && !Boolean.FALSE.equals(r.getActivo()))
                .orElseThrow(() -> new EntityNotFoundException("Restaurante no encontrado"));
        return espacioRepo.findAllByRestaurant_IdAndActivoTrueOrderByIdAsc(restaurantId).stream()
                .map(e -> new EspacioPublicoDTO(e.getId(), e.getTipo(), e.getNombre(),
                        fecha == null ? null : personasReservadas(e, fecha) + cantidad <= cupoDelDia(e, fecha)))
                .toList();
    }

    /**
     * Espacio activo del restaurante donde el cliente quiere reservar.
     *
     * @throws BusinessException si no se indicó, no es de ese restaurante o está desactivado
     */
    @Transactional(readOnly = true)
    public Espacio paraReservar(Restaurant restaurant, Long espacioId) {
        if (espacioId == null) {
            throw new BusinessException("Elegí en qué espacio querés reservar");
        }
        return espacioRepo.findById(espacioId)
                .filter(e -> e.getRestaurant().getId().equals(restaurant.getId()))
                .filter(e -> Boolean.TRUE.equals(e.getActivo()))
                .orElseThrow(() -> new BusinessException("Ese espacio no está disponible en este restaurante"));
    }

    /**
     * Cupo que rige ese día en el espacio: su cupo especial si lo tiene (nunca mayor a la capacidad
     * actual), si no su capacidad normal.
     */
    @Transactional(readOnly = true)
    public int cupoDelDia(Espacio espacio, LocalDate fecha) {
        return cupoRepo.findByEspacio_IdAndFecha(espacio.getId(), fecha)
                .map(CupoEspecial::getCupo)
                .map(cupo -> Math.min(cupo, espacio.getCapacidad()))
                .orElse(espacio.getCapacidad());
    }

    /** Personas con reservas no canceladas en ese espacio ese día. */
    @Transactional(readOnly = true)
    public int personasReservadas(Espacio espacio, LocalDate fecha) {
        return reservaRepo.findAllByEspacio_IdAndFechaReserva(espacio.getId(), comoFechaReserva(fecha)).stream()
                .filter(r -> r.getEstadoReserva() != EstadoReserva.CANCELADA)
                .mapToInt(Reserva::getCantidad)
                .sum();
    }

    /**
     * Recalcula {@code cupoMaximo} como la suma de los espacios activos. Se mantiene como columna
     * porque admin, moderación y el panel lo muestran como capacidad total.
     */
    @Transactional
    public void recalcularCapacidadTotal(Restaurant restaurant) {
        espacioRepo.flush();
        restaurant.setCupoMaximo(espacioRepo.capacidadTotal(restaurant.getId()));
        restaurantRepo.save(restaurant);
    }

    /** {@code fechaReserva} se guarda como medianoche en la zona horaria del servidor. */
    public static Date comoFechaReserva(LocalDate fecha) {
        return Date.from(fecha.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    /**
     * Espacio del restaurante, activo o no.
     *
     * @throws EntityNotFoundException si no existe o es de otro restaurante
     */
    @Transactional(readOnly = true)
    public Espacio delRestaurante(Restaurant restaurant, Long espacioId) {
        return espacioRepo.findById(espacioId)
                .filter(e -> e.getRestaurant().getId().equals(restaurant.getId()))
                .orElseThrow(() -> new EntityNotFoundException("Espacio no encontrado"));
    }

    /** Mayor cantidad de personas reservadas (no canceladas) en un mismo día, de hoy en adelante. */
    private int maximoReservadoFuturo(Espacio espacio) {
        Map<Date, Integer> porDia = reservaRepo
                .findAllByEspacio_IdAndFechaReservaGreaterThanEqual(espacio.getId(), comoFechaReserva(LocalDate.now()))
                .stream()
                .filter(r -> r.getEstadoReserva() != EstadoReserva.CANCELADA)
                .collect(Collectors.groupingBy(Reserva::getFechaReserva, Collectors.summingInt(Reserva::getCantidad)));
        return porDia.values().stream().mapToInt(Integer::intValue).max().orElse(0);
    }

    /** Valida el cuerpo y devuelve el nombre normalizado. */
    private String validar(Restaurant restaurant, EspacioRequestDTO request, Long espacioId) {
        if (request == null) {
            throw new BusinessException("No hay datos del espacio");
        }
        if (request.tipo() == null) {
            throw new BusinessException("Indicá el tipo de espacio: SALON, PATIO o BARRA");
        }
        if (request.nombre() == null || request.nombre().isBlank()) {
            throw new BusinessException("Indicá el nombre del espacio");
        }
        String nombre = request.nombre().strip();
        if (nombre.length() > Espacio.MAX_NOMBRE) {
            throw new BusinessException("El nombre del espacio puede tener hasta %d caracteres".formatted(Espacio.MAX_NOMBRE));
        }
        if (request.capacidad() == null || request.capacidad() < 1) {
            throw new BusinessException("La capacidad del espacio debe ser de al menos 1 persona");
        }
        boolean repetido = espacioRepo.findAllByRestaurant_IdOrderByIdAsc(restaurant.getId()).stream()
                .filter(e -> !e.getId().equals(espacioId))
                .anyMatch(e -> e.getNombre().equalsIgnoreCase(nombre));
        if (repetido) {
            throw new BusinessException("Ya tenés un espacio con ese nombre");
        }
        return nombre;
    }
}
