package com.example.Foodies.Resena;

import com.example.Foodies.Exception.BusinessException;
import com.example.Foodies.Exception.EntityNotFoundException;
import com.example.Foodies.Enums.Role;
import com.example.Foodies.Restaurant.Restaurant;
import com.example.Foodies.Restaurant.RestaurantRepository;
import com.example.Foodies.Resena.dtos.ResenaDetailDTO;
import com.example.Foodies.Resena.dtos.ResenaListDTO;
import com.example.Foodies.Resena.dtos.ResenaPatchDTO;
import com.example.Foodies.Resena.dtos.ResenaRequestDTO;
import com.example.Foodies.Usuario.Usuario;
import com.example.Foodies.Usuario.UsuarioRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;


@Service
public class ResenaService {

    @Autowired
    private ResenaRepository resenaRepo;

    @Autowired
    private UsuarioRepository usuarioRepo;

    @Autowired
    private RestaurantRepository restauranteRepo;
    @Autowired
    private  ResenaMapper resenaMapper;

    @Transactional
    public ResenaDetailDTO createResena(ResenaRequestDTO resena) {
        // Primero lo que viene del request: si falta algo, 400 antes de tocar la base
        if (resena == null || resena.getRestaurantId() == null) {
            throw new BusinessException("El restaurante es obligatorio");
        }
        if (resena.getComentario() == null || resena.getComentario().isBlank()) {
            throw new BusinessException("El comentario es obligatorio");
        }

        // El autor siempre es el usuario del token: no se acepta reseñar en nombre de otro
        Usuario usuario = usuarioActual();

        Restaurant restaurante = restauranteRepo.findById(resena.getRestaurantId())
                .orElseThrow(() -> new EntityNotFoundException("Restaurante no encontrado"));

        boolean yaExiste = resenaRepo.existsByUsuario_IdAndRestaurant_Id(usuario.getId(), restaurante.getId());
        if (yaExiste) {
            throw new BusinessException("El usuario ya realizó una reseña para este restaurante.");
        }

        if (resena.getCalificacion() < 1 || resena.getCalificacion() > 5) {
            throw new BusinessException("La calificación debe estar entre 1 y 5.");
        }

        Resena resenaNueva = resenaMapper.toEntity(resena);
        resenaNueva.setUsuario(usuario);
        resenaNueva.setRestaurant(restaurante);

        resenaNueva = resenaRepo.save(resenaNueva);

        return resenaMapper.toDto(resenaNueva);
    }

    /** Reseñas de un restaurante, paginadas. */
    public Page<ResenaListDTO> getAllResenasByRestaurant(Long id, Pageable pageable) {
        verificarAccesoAlRestaurante(id);

        return resenaRepo.findByRestaurant_Id(id, pageable).map(resenaMapper::toListDto);
    }

    public ResenaDetailDTO getResenaById(Long id) {
        Resena r = resenaRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Reseña no encontrada"));

        if (esEncargado(usuarioActual())) {
            verificarQueSeaSuRestaurante(r.getRestaurant());
        }

        return new ResenaDetailDTO(
                r.getId(),
                r.getComentario(),
                r.getCalificacion(),
                r.getUsuario().getNombre(),
                r.getRestaurant().getNombre()
        );
    }

    public ResenaDetailDTO updateResena(Long id, ResenaPatchDTO dto) {
        Resena r = resenaRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Reseña no encontrada"));

        verificarPropietario(r);

        if (dto.getComentario() != null) {
            r.setComentario(dto.getComentario());
        }
        if (dto.getCalificacion() != null) {
            int calificacion = dto.getCalificacion();
            if (calificacion < 1 || calificacion > 5) {
                throw new BusinessException("La calificación debe estar entre 1 y 5.");
            }
            r.setCalificacion(calificacion);
        }

        r = resenaRepo.save(r);

        return new ResenaDetailDTO(
                r.getId(),
                r.getComentario(),
                r.getCalificacion(),
                r.getUsuario().getNombre(),
                r.getRestaurant().getNombre()
        );
    }

    public void deleteResena(Long id) {
        Resena r = resenaRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("La reseña no existe."));

        // Solo el dueño de la reseña o un admin pueden eliminarla
        Usuario usuarioActual = usuarioActual();
        if (!esAdmin(usuarioActual)
                && (r.getUsuario() == null || !r.getUsuario().getId().equals(usuarioActual.getId()))) {
            throw new AccessDeniedException("No podés eliminar una reseña que no es tuya");
        }

        resenaRepo.delete(r);
    }

    /** Reseñas de un usuario, paginadas. */
    public Page<ResenaListDTO> getallResenaByUsuario (Long id, Pageable pageable){
        return resenaRepo.findByUsuario_Id(id, pageable).map(resenaMapper::toListDto);
    }

    private void verificarPropietario(Resena resena) {
        Usuario usuarioActual = usuarioActual();
        if (esAdmin(usuarioActual)) {
            return;
        }
        if (resena.getUsuario() == null || !resena.getUsuario().getId().equals(usuarioActual.getId())) {
            throw new AccessDeniedException("No podés editar una reseña que no es tuya");
        }
    }

    private Usuario usuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof String email) || email.isBlank()) {
            throw new AccessDeniedException("No autenticado");
        }
        return usuarioRepo.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));
    }

    private boolean esAdmin(Usuario usuario) {
        return usuario.getRol() == Role.ROLE_ADMIN;
    }

    private boolean esEncargado(Usuario usuario) {
        return usuario.getRol() == Role.ROLE_ENCARGADO;
    }

    /** El encargado solo puede leer las reseñas de SU restaurante; el cliente lee las que quiera */
    private void verificarAccesoAlRestaurante(Long restauranteId) {
        Usuario actual = usuarioActual();
        if (!esEncargado(actual)) {
            return;
        }
        Restaurant restaurante = restauranteRepo.findById(restauranteId)
                .orElseThrow(() -> new EntityNotFoundException("Restaurante no encontrado"));
        verificarQueSeaSuRestaurante(restaurante);
    }

    private void verificarQueSeaSuRestaurante(Restaurant restaurante) {
        Usuario actual = usuarioActual();
        if (esAdmin(actual)) {
            return;
        }
        if (restaurante == null || restaurante.getUsuario() == null
                || !restaurante.getUsuario().getId().equals(actual.getId())) {
            throw new AccessDeniedException("No podés ver las reseñas de un restaurante que no es tuyo");
        }
    }
}