package com.example.Foodies.Restaurant;

import com.example.Foodies.Carta.CartaService;
import com.example.Foodies.Carta.Dtos.CartaDTO;
import com.example.Foodies.Config.CurrentUser;
import com.example.Foodies.Enums.EstadoReserva;
import com.example.Foodies.Exception.BusinessException;
import com.example.Foodies.Exception.EntityNotFoundException;
import com.example.Foodies.Exception.ForbiddenException;
import com.example.Foodies.Reserva.Reserva;
import com.example.Foodies.Reserva.ReservaRepository;
import com.example.Foodies.Reserva.dtos.ReservaPanelDTO;
import com.example.Foodies.Restaurant.Dtos.MiRestauranteDTO;
import com.example.Foodies.Restaurant.Dtos.RestaurantPatchDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Comparator;
import java.util.List;

/** Everything a manager does in their panel, always scoped to the restaurant of the authenticated user. */
@Service
public class RestaurantePanelService {

    private final RestaurantRepository restaurantRepo;
    private final ReservaRepository reservaRepo;
    private final CartaService cartaService;
    private final CurrentUser currentUser;

    public RestaurantePanelService(RestaurantRepository restaurantRepo, ReservaRepository reservaRepo,
                                   CartaService cartaService, CurrentUser currentUser) {
        this.restaurantRepo = restaurantRepo;
        this.reservaRepo = reservaRepo;
        this.cartaService = cartaService;
        this.currentUser = currentUser;
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
        if (patch.getCupoMaximo() != null) {
            restaurant.setCupoMaximo(patch.getCupoMaximo());
        }
        if (patch.getEspecialidad() != null) {
            restaurant.setEspecialidad(patch.getEspecialidad());
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

    @Transactional
    public ReservaPanelDTO cambiarEstadoReserva(Long reservaId, EstadoReserva estado) {
        if (estado != EstadoReserva.ACEPTADA && estado != EstadoReserva.CANCELADA) {
            throw new BusinessException("El estado debe ser ACEPTADA o CANCELADA");
        }
        Reserva reserva = reservaRepo.findById(reservaId)
                .orElseThrow(() -> new EntityNotFoundException("Reserva no encontrada"));
        if (reserva.getRestaurant() == null || !reserva.getRestaurant().getId().equals(miRestaurante().getId())) {
            throw new ForbiddenException("La reserva no pertenece a tu restaurante");
        }
        reserva.setEstadoReserva(estado);
        return ReservaPanelDTO.from(reservaRepo.save(reserva));
    }

    private Restaurant miRestaurante() {
        return restaurantRepo.findByUsuario_Email(currentUser.email())
                .orElseThrow(() -> new EntityNotFoundException("No tenés un restaurante asociado"));
    }

    private MiRestauranteDTO toDto(Restaurant restaurant) {
        return new MiRestauranteDTO(restaurant.getId(), restaurant.getNombre(), restaurant.getCupoMaximo(),
                restaurant.getUbicacion(), restaurant.getEspecialidad(),
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
        if (patch.getCupoMaximo() != null && patch.getCupoMaximo() < 1) {
            throw new BusinessException("El cupo máximo debe ser al menos 1");
        }
    }
}
