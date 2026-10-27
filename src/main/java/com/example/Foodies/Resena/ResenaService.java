package com.example.Foodies.Resena;

import com.example.Foodies.Exception.BusinessException;
import com.example.Foodies.Exception.EntityNotFoundException;
import com.example.Foodies.Exception.ForbiddenException;
import com.example.Foodies.Exception.ListNoContentException;
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
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ResenaService {

    @Autowired
    private ResenaRepository resenaRepo;

    @Autowired
    private UsuarioRepository usuarioRepo;

    @Autowired
    private RestaurantRepository restauranteRepo;

    @Autowired
    private ResenaMapper resenaMapper;

    @Transactional
    public ResenaDetailDTO createResena(ResenaRequestDTO resena, String email) {
        Usuario usuario = usuarioRepo.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));

        Restaurant restaurante = restauranteRepo.findById(resena.getRestaurantId())
                .orElseThrow(() -> new EntityNotFoundException("Restaurante no encontrado"));

        boolean yaExiste = resenaRepo.existsByUsuario_IdAndRestaurant_Id(
                usuario.getId(),
                restaurante.getId()
        );
        if (yaExiste) {
            throw new BusinessException("Ya realizaste una reseña para este restaurante.");
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

    public List<ResenaListDTO> getAllResenasByRestaurant(Long id) {
        List<Resena> resenaList = resenaRepo.findByRestaurant_Id(id);
        if (resenaList.isEmpty()) {
            throw new ListNoContentException("Este restaurante no tiene reseñas");
        }
        return resenaMapper.toListDtoList(resenaList);
    }

    public ResenaDetailDTO getResenaById(Long id) {
        Resena resena = buscarResena(id);

        return new ResenaDetailDTO(
                resena.getId(),
                resena.getComentario(),
                resena.getCalificacion(),
                resena.getUsuario().getNombre(),
                resena.getRestaurant().getNombre()
        );
    }

    @Transactional
    public ResenaDetailDTO updateResena(Long id, ResenaPatchDTO dto, String email, boolean admin) {
        Resena resena = buscarResena(id);
        verificarPropietario(resena, email, admin);

        if (dto.getComentario() != null) {
            resena.setComentario(dto.getComentario());
        }
        if (dto.getCalificacion() != null) {
            int calificacion = dto.getCalificacion();
            if (calificacion < 1 || calificacion > 5) {
                throw new BusinessException("La calificación debe estar entre 1 y 5.");
            }
            resena.setCalificacion(calificacion);
        }

        resena = resenaRepo.save(resena);

        return new ResenaDetailDTO(
                resena.getId(),
                resena.getComentario(),
                resena.getCalificacion(),
                resena.getUsuario().getNombre(),
                resena.getRestaurant().getNombre()
        );
    }

    @Transactional
    public void deleteResena(Long id, String email, boolean admin) {
        Resena resena = buscarResena(id);
        verificarPropietario(resena, email, admin);
        resenaRepo.delete(resena);
    }

    public List<ResenaListDTO> getallResenaByUsuario(Long id) {
        List<Resena> resenas = resenaRepo.findByUsuario_Id(id);
        if (resenas.isEmpty()) {
            throw new ListNoContentException("Este usuario no tiene reseñas");
        }
        return resenaMapper.toListDtoList(resenas);
    }

    private Resena buscarResena(Long id) {
        return resenaRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Reseña no encontrada"));
    }

    private void verificarPropietario(Resena resena, String email, boolean admin) {
        if (admin) {
            return;
        }
        Usuario autor = resena.getUsuario();
        if (autor == null || autor.getEmail() == null || !autor.getEmail().equalsIgnoreCase(email)) {
            throw new ForbiddenException("No podés modificar una reseña de otro usuario.");
        }
    }
}
