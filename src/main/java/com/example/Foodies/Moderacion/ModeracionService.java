package com.example.Foodies.Moderacion;

import com.example.Foodies.Enums.EstadoRestaurante;
import com.example.Foodies.Enums.Role;
import com.example.Foodies.Exception.BusinessException;
import com.example.Foodies.Exception.EntityNotFoundException;
import com.example.Foodies.Moderacion.dtos.EstadoSolicitudDTO;
import com.example.Foodies.Moderacion.dtos.SolicitudRestauranteDTO;
import com.example.Foodies.Moderacion.dtos.StrikeResultadoDTO;
import com.example.Foodies.Moderacion.dtos.UbicacionRequestDTO;
import com.example.Foodies.Restaurant.Restaurant;
import com.example.Foodies.Usuario.Usuario;
import com.example.Foodies.Usuario.UsuarioRepository;
import com.example.Foodies.Usuario.UsuarioService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Casos de uso de moderación de la Fase 3: solicitudes de restaurantes, strikes y ubicación en el mapa.
 *
 * <p>No modifica los servicios de autenticación ni de reservas: para aprobar reutiliza
 * {@link UsuarioService#aprobarEncargado(Long)}, que también asigna el rol de encargado.</p>
 */
@Service
public class ModeracionService {

    /** Strikes a partir de los cuales el panel sugiere dar de baja la cuenta. */
    public static final int LIMITE_STRIKES = 3;

    private final ModeracionRestaurantRepository restaurantRepo;
    private final UsuarioRepository usuarioRepo;
    private final UsuarioService usuarioService;
    private final EmailService emailService;

    public ModeracionService(ModeracionRestaurantRepository restaurantRepo,
                             UsuarioRepository usuarioRepo,
                             UsuarioService usuarioService,
                             EmailService emailService) {
        this.restaurantRepo = restaurantRepo;
        this.usuarioRepo = usuarioRepo;
        this.usuarioService = usuarioService;
        this.emailService = emailService;
    }

    /**
     * Lista las solicitudes de alta que esperan revisión.
     *
     * @return solicitudes en estado PENDIENTE, de la más antigua a la más nueva
     */
    @Transactional(readOnly = true)
    public List<SolicitudRestauranteDTO> solicitudesPendientes() {
        return restaurantRepo.findSolicitudes(EstadoRestaurante.PENDIENTE);
    }

    /**
     * Aprueba una solicitud: publica el restaurante y le da al encargado el rol {@code ROLE_ENCARGADO}.
     * También sirve para aprobar una solicitud rechazada antes.
     *
     * @param restaurantId id del restaurante
     * @return el estado final de la solicitud
     * @throws EntityNotFoundException si el restaurante no existe
     */
    @Transactional
    public EstadoSolicitudDTO aprobar(Long restaurantId) {
        Restaurant restaurant = buscarRestaurante(restaurantId);
        usuarioService.aprobarEncargado(restaurant.getUsuario().getId());
        restaurant.setEstado(EstadoRestaurante.APROBADO);
        return new EstadoSolicitudDTO(restaurant.getId(), restaurant.getNombre(), restaurant.getEstado());
    }

    /**
     * Rechaza una solicitud pendiente. El restaurante no se publica y la cuenta sigue sin rol de encargado.
     *
     * @param restaurantId id del restaurante
     * @return el estado final de la solicitud
     * @throws EntityNotFoundException si el restaurante no existe
     * @throws BusinessException       si el restaurante ya estaba aprobado (darlo de baja es otra acción)
     */
    @Transactional
    public EstadoSolicitudDTO rechazar(Long restaurantId) {
        Restaurant restaurant = buscarRestaurante(restaurantId);
        if (restaurant.getEstado() == EstadoRestaurante.APROBADO) {
            throw new BusinessException("Solo se pueden rechazar solicitudes pendientes");
        }
        restaurant.setAprobado(false);
        restaurant.setEstado(EstadoRestaurante.RECHAZADO);
        return new EstadoSolicitudDTO(restaurant.getId(), restaurant.getNombre(), restaurant.getEstado());
    }

    /**
     * Suma un strike a un usuario y le envía el aviso por email.
     *
     * @param usuarioId id del usuario
     * @param motivo    motivo que recibe el usuario en el aviso
     * @return strikes acumulados, sugerencia de baja y si el email salió
     * @throws EntityNotFoundException si el usuario no existe
     * @throws BusinessException       si el usuario es administrador
     */
    @Transactional
    public StrikeResultadoDTO strikeUsuario(Long usuarioId, String motivo) {
        Usuario usuario = usuarioRepo.findById(usuarioId)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));
        if (usuario.getRol() == Role.ROLE_ADMIN) {
            throw new BusinessException("No se puede sancionar a un administrador");
        }
        usuario.setStrikes(usuario.getStrikes() + 1);
        String nombre = Stream.of(usuario.getNombre(), usuario.getApellido())
                .filter(parte -> parte != null && !parte.isBlank())
                .collect(Collectors.joining(" "));
        return notificar(usuario.getId(), nombre, usuario.getEmail(), usuario.getStrikes(), motivo);
    }

    /**
     * Suma un strike a un restaurante y envía el aviso al email de su encargado.
     *
     * @param restaurantId id del restaurante
     * @param motivo       motivo que recibe el encargado en el aviso
     * @return strikes acumulados, sugerencia de baja y si el email salió
     * @throws EntityNotFoundException si el restaurante no existe
     */
    @Transactional
    public StrikeResultadoDTO strikeRestaurante(Long restaurantId, String motivo) {
        Restaurant restaurant = buscarRestaurante(restaurantId);
        restaurant.setStrikes(restaurant.getStrikes() + 1);
        return notificar(restaurant.getId(), restaurant.getNombre(), restaurant.getUsuario().getEmail(),
                restaurant.getStrikes(), motivo);
    }

    /**
     * Guarda las coordenadas del restaurante para mostrarlo en el mapa.
     *
     * @param restaurantId id del restaurante
     * @param ubicacion    latitud y longitud validadas
     * @throws EntityNotFoundException si el restaurante no existe
     */
    @Transactional
    public void actualizarUbicacion(Long restaurantId, UbicacionRequestDTO ubicacion) {
        Restaurant restaurant = buscarRestaurante(restaurantId);
        restaurant.setLatitud(ubicacion.latitud());
        restaurant.setLongitud(ubicacion.longitud());
    }

    private StrikeResultadoDTO notificar(Long id, String nombre, String email, int strikes, String motivo) {
        // Mail failures are logged and reported in the response; they never roll back the strike.
        boolean enviado = emailService.enviarAvisoSancion(email, nombre, strikes, LIMITE_STRIKES, motivo.trim());
        return new StrikeResultadoDTO(id, nombre, email, strikes, strikes >= LIMITE_STRIKES, enviado);
    }

    private Restaurant buscarRestaurante(Long restaurantId) {
        return restaurantRepo.findById(restaurantId)
                .orElseThrow(() -> new EntityNotFoundException("Restaurante no encontrado"));
    }
}
