package com.example.Foodies.Moderacion;

import com.example.Foodies.Enums.EstadoRestaurante;
import com.example.Foodies.Enums.Role;
import com.example.Foodies.Exception.BusinessException;
import com.example.Foodies.Exception.ConflictException;
import com.example.Foodies.Exception.EntityNotFoundException;
import com.example.Foodies.Foto.FotoService;
import com.example.Foodies.Moderacion.dtos.EstadoSolicitudDTO;
import com.example.Foodies.Moderacion.dtos.HistorialStrikeDTO;
import com.example.Foodies.Moderacion.dtos.SolicitudRestauranteDTO;
import com.example.Foodies.Moderacion.dtos.StrikeResultadoDTO;
import com.example.Foodies.Moderacion.dtos.StrikesDTO;
import com.example.Foodies.Moderacion.dtos.UbicacionRequestDTO;
import com.example.Foodies.Restaurant.Restaurant;
import com.example.Foodies.Usuario.Usuario;
import com.example.Foodies.Usuario.UsuarioRepository;
import com.example.Foodies.Usuario.UsuarioService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
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
    private final FotoService fotoService;
    private final HistorialStrikeRepository historialRepo;
    private final Clock clock;

    /** Longest reason the strike history keeps. */
    public static final int MAX_MOTIVO = 255;

    public ModeracionService(ModeracionRestaurantRepository restaurantRepo,
                             UsuarioRepository usuarioRepo,
                             UsuarioService usuarioService,
                             EmailService emailService,
                             FotoService fotoService,
                             HistorialStrikeRepository historialRepo,
                             Clock clock) {
        this.restaurantRepo = restaurantRepo;
        this.usuarioRepo = usuarioRepo;
        this.usuarioService = usuarioService;
        this.emailService = emailService;
        this.fotoService = fotoService;
        this.historialRepo = historialRepo;
        this.clock = clock;
    }

    /**
     * Lista las solicitudes de alta que esperan revisión.
     *
     * @return solicitudes en estado PENDIENTE con sus fotos, de la más antigua a la más nueva
     */
    @Transactional(readOnly = true)
    public List<SolicitudRestauranteDTO> solicitudesPendientes() {
        return restaurantRepo.findSolicitudes(EstadoRestaurante.PENDIENTE).stream()
                .map(solicitud -> solicitud.conFotos(fotoService.listar(solicitud.restaurantId())))
                .toList();
    }

    /**
     * Aprueba una solicitud: publica el restaurante y le da al encargado el rol {@code ROLE_ENCARGADO}.
     * También sirve para aprobar una solicitud rechazada antes. Después del commit le envía al
     * encargado el email de aprobación; si el restaurante ya estaba aprobado no lo repite.
     *
     * @param restaurantId id del restaurante
     * @return el estado final de la solicitud
     * @throws EntityNotFoundException si el restaurante no existe
     */
    @Transactional
    public EstadoSolicitudDTO aprobar(Long restaurantId) {
        Restaurant restaurant = buscarRestaurante(restaurantId);
        boolean yaEstabaAprobado = restaurant.getEstado() == EstadoRestaurante.APROBADO;
        usuarioService.aprobarEncargado(restaurant.getUsuario().getId());
        restaurant.setEstado(EstadoRestaurante.APROBADO);
        if (!yaEstabaAprobado) {
            String email = restaurant.getUsuario().getEmail();
            String nombre = restaurant.getNombre();
            DespuesDelCommit.ejecutar(() -> emailService.enviarRestauranteAprobado(email, nombre));
        }
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
        validarActiva(usuario.getActivo());
        validarLimite(usuario.getStrikes());
        usuario.setStrikes(usuario.getStrikes() + 1);
        registrar(usuario, null, HistorialStrike.Tipo.SUMA, HistorialStrike.Origen.ADMIN, motivo, usuario.getStrikes());
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
        validarActiva(restaurant.getActivo());
        validarLimite(restaurant.getStrikes());
        restaurant.setStrikes(restaurant.getStrikes() + 1);
        registrar(null, restaurant, HistorialStrike.Tipo.SUMA, HistorialStrike.Origen.ADMIN, motivo, restaurant.getStrikes());
        return notificar(restaurant.getId(), restaurant.getNombre(), restaurant.getUsuario().getEmail(),
                restaurant.getStrikes(), motivo);
    }

    /**
     * Removes one strike from a user (S7c, D16). No email is sent; the change is kept in the history.
     * A client that drops below the limit can book again (D6 reads the counter).
     *
     * @return the strikes left
     * @throws EntityNotFoundException if the user does not exist
     * @throws BusinessException       if the reason is blank or too long
     * @throws ConflictException       if the user has no strikes
     */
    @Transactional
    public StrikesDTO quitarStrikeUsuario(Long usuarioId, String motivo) {
        Usuario usuario = usuarioRepo.findById(usuarioId)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));
        String limpio = motivoValido(motivo);
        int strikes = validarConStrikes(usuario.getStrikes());
        usuario.setStrikes(strikes - 1);
        registrar(usuario, null, HistorialStrike.Tipo.QUITA, HistorialStrike.Origen.ADMIN, limpio, usuario.getStrikes());
        return new StrikesDTO(usuario.getStrikes());
    }

    /** Removes one strike from a restaurant (S7c); same rules as {@link #quitarStrikeUsuario}. */
    @Transactional
    public StrikesDTO quitarStrikeRestaurante(Long restaurantId, String motivo) {
        Restaurant restaurant = buscarRestaurante(restaurantId);
        String limpio = motivoValido(motivo);
        int strikes = validarConStrikes(restaurant.getStrikes());
        restaurant.setStrikes(strikes - 1);
        registrar(null, restaurant, HistorialStrike.Tipo.QUITA, HistorialStrike.Origen.ADMIN, limpio, restaurant.getStrikes());
        return new StrikesDTO(restaurant.getStrikes());
    }

    /** Strike history of a user, newest first (S7d). */
    @Transactional(readOnly = true)
    public List<HistorialStrikeDTO> historialUsuario(Long usuarioId) {
        if (!usuarioRepo.existsById(usuarioId)) {
            throw new EntityNotFoundException("Usuario no encontrado");
        }
        return historialRepo.findAllByUsuario_IdOrderByFechaDescIdDesc(usuarioId).stream()
                .map(HistorialStrikeDTO::from)
                .toList();
    }

    /** Strike history of a restaurant, newest first (S7d). */
    @Transactional(readOnly = true)
    public List<HistorialStrikeDTO> historialRestaurante(Long restaurantId) {
        buscarRestaurante(restaurantId);
        return historialRepo.findAllByRestaurant_IdOrderByFechaDescIdDesc(restaurantId).stream()
                .map(HistorialStrikeDTO::from)
                .toList();
    }

    private static String motivoValido(String motivo) {
        if (motivo == null || motivo.isBlank()) {
            throw new BusinessException("MOTIVO_OBLIGATORIO", java.util.Map.of(), "Indicá el motivo");
        }
        String limpio = motivo.strip();
        if (limpio.length() > MAX_MOTIVO) {
            throw new BusinessException("MOTIVO_MUY_LARGO", java.util.Map.of("maximo", MAX_MOTIVO),
                    "El motivo puede tener hasta %d caracteres".formatted(MAX_MOTIVO));
        }
        return limpio;
    }

    private static int validarConStrikes(Integer strikes) {
        if (strikes == null || strikes <= 0) {
            throw new ConflictException("SIN_STRIKES", java.util.Map.of(), "No tiene strikes para quitar");
        }
        return strikes;
    }

    /** Keeps one change of a strike counter in the history (S7d). */
    private void registrar(Usuario usuario, Restaurant restaurant, HistorialStrike.Tipo tipo,
                           HistorialStrike.Origen origen, String motivo, int totalDespues) {
        HistorialStrike entrada = new HistorialStrike();
        entrada.setUsuario(usuario);
        entrada.setRestaurant(restaurant);
        entrada.setFecha(LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS));
        entrada.setTipo(tipo);
        entrada.setOrigen(origen);
        entrada.setMotivo(motivo.strip());
        entrada.setTotalDespues(totalDespues);
        historialRepo.save(entrada);
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

    private static void validarActiva(Boolean activo) {
        if (Boolean.FALSE.equals(activo)) {
            throw new BusinessException("CUENTA_DADA_DE_BAJA", java.util.Map.of(), "No se puede sancionar una cuenta dada de baja");
        }
    }

    /** No-shows that add one automatic strike (D5). */
    public static final int AUSENCIAS_POR_STRIKE = 3;

    /**
     * Counts a no-show marked by a restaurant. The third one adds a strike (unless the account is
     * deactivated or already at the limit) and restarts the count; the client is told by email after
     * the commit.
     *
     * @return {@code true} if this no-show added a strike
     */
    @Transactional
    public boolean registrarAusencia(Usuario cliente) {
        int ausencias = (cliente.getAusencias() == null ? 0 : cliente.getAusencias()) + 1;
        if (ausencias < AUSENCIAS_POR_STRIKE) {
            cliente.setAusencias(ausencias);
            return false;
        }
        cliente.setAusencias(0);
        int strikes = cliente.getStrikes() == null ? 0 : cliente.getStrikes();
        if (Boolean.FALSE.equals(cliente.getActivo()) || strikes >= LIMITE_STRIKES) {
            return false;
        }
        cliente.setStrikes(strikes + 1);
        String email = cliente.getEmail();
        String nombre = Stream.of(cliente.getNombre(), cliente.getApellido())
                .filter(parte -> parte != null && !parte.isBlank())
                .collect(Collectors.joining(" "));
        int total = cliente.getStrikes();
        String motivo = "No asististe a %d reservas sin avisar.".formatted(AUSENCIAS_POR_STRIKE);
        registrar(cliente, null, HistorialStrike.Tipo.SUMA, HistorialStrike.Origen.AUSENCIAS, motivo, total);
        DespuesDelCommit.ejecutar(() -> emailService.enviarAvisoSancion(email, nombre, total, LIMITE_STRIKES, motivo));
        return true;
    }

    private void validarLimite(int strikes) {
        if (strikes >= LIMITE_STRIKES) {
            throw new BusinessException(
                    "Ya llegó al límite de %d strikes: el siguiente paso es darlo de baja".formatted(LIMITE_STRIKES));
        }
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
