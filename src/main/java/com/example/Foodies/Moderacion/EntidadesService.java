package com.example.Foodies.Moderacion;

import com.example.Foodies.Enums.Role;
import com.example.Foodies.Exception.BusinessException;
import com.example.Foodies.Exception.EntityNotFoundException;
import com.example.Foodies.Moderacion.dtos.EntidadClienteDTO;
import com.example.Foodies.Moderacion.dtos.EntidadRestauranteDTO;
import com.example.Foodies.Moderacion.dtos.EstadoActivoDTO;
import com.example.Foodies.Restaurant.Restaurant;
import com.example.Foodies.Usuario.Usuario;
import com.example.Foodies.Usuario.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/**
 * "Gestión de Entidades" de la Fase 3: listado de clientes y restaurantes y su baja lógica.
 *
 * <p>La baja nunca borra filas: solo cambia {@code activo}, así reservas y reseñas conservan sus
 * relaciones. Qué implica estar dado de baja lo aplican {@link CuentaActivaFilter} (usuarios) y el
 * catálogo público (restaurantes).</p>
 */
@Service
public class EntidadesService {

    private final UsuarioRepository usuarioRepo;
    private final ModeracionRestaurantRepository restaurantRepo;

    public EntidadesService(UsuarioRepository usuarioRepo, ModeracionRestaurantRepository restaurantRepo) {
        this.usuarioRepo = usuarioRepo;
        this.restaurantRepo = restaurantRepo;
    }

    /**
     * @return los clientes ordenados por id, con strikes y baja lógica
     */
    @Transactional(readOnly = true)
    public List<EntidadClienteDTO> clientes() {
        return usuarioRepo.findByRol(Role.ROLE_CLIENTE).stream()
                .sorted(Comparator.comparing(Usuario::getId))
                .map(u -> new EntidadClienteDTO(u.getId(), u.getNombre(), u.getApellido(), u.getEmail(),
                        u.getTelefono(), u.getStrikes(), u.getActivo(), u.getFechaRegistro()))
                .toList();
    }

    /**
     * @return todos los restaurantes, en cualquier estado, ordenados por id
     */
    @Transactional(readOnly = true)
    public List<EntidadRestauranteDTO> restaurantes() {
        return restaurantRepo.findEntidades();
    }

    /**
     * Da de baja o reactiva una cuenta de usuario.
     *
     * @param usuarioId id del usuario
     * @param activo    {@code false} para dar de baja
     * @return el estado en el que quedó
     * @throws EntityNotFoundException si el usuario no existe
     * @throws BusinessException       si es un administrador (dejaría el panel sin acceso)
     */
    @Transactional
    public EstadoActivoDTO cambiarActivoUsuario(Long usuarioId, boolean activo) {
        Usuario usuario = usuarioRepo.findById(usuarioId)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));
        if (usuario.getRol() == Role.ROLE_ADMIN) {
            throw new BusinessException("No se puede dar de baja a un administrador");
        }
        usuario.setActivo(activo);
        String nombre = usuario.getNombre() == null ? usuario.getEmail() : usuario.getNombre();
        return new EstadoActivoDTO(usuario.getId(), nombre, activo);
    }

    /**
     * Da de baja o reactiva un restaurante. Dado de baja deja de aparecer en el catálogo público;
     * la cuenta del encargado no cambia.
     *
     * @param restaurantId id del restaurante
     * @param activo       {@code false} para dar de baja
     * @return el estado en el que quedó
     * @throws EntityNotFoundException si el restaurante no existe
     */
    @Transactional
    public EstadoActivoDTO cambiarActivoRestaurante(Long restaurantId, boolean activo) {
        Restaurant restaurant = restaurantRepo.findById(restaurantId)
                .orElseThrow(() -> new EntityNotFoundException("Restaurante no encontrado"));
        restaurant.setActivo(activo);
        return new EstadoActivoDTO(restaurant.getId(), restaurant.getNombre(), activo);
    }
}
