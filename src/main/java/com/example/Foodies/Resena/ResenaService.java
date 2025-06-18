package com.example.Foodies.Resena;

import com.example.Foodies.Cliente.Cliente;
import com.example.Foodies.Cliente.ClienteRepository;
import com.example.Foodies.Restaurant.Restaurant;
import com.example.Foodies.Restaurant.RestaurantRepository;
import com.example.Foodies.Resena.dtos.ResenaDetailDTO;
import com.example.Foodies.Resena.dtos.ResenaListDTO;
import com.example.Foodies.Resena.dtos.ResenaPatchDTO;
import com.example.Foodies.Resena.dtos.ResenaRequestDTO;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ResenaService {

    @Autowired
    private ResernaRepository resenaRepo;

    @Autowired
    private ClienteRepository clienteRepo;

    @Autowired
    private RestaurantRepository restauranteRepo;

    @Transactional
    public Resena createResena(Resena resena) {
        Cliente cliente = clienteRepo.findById(resena.getCliente().getId())
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

        Restaurant restaurante = restauranteRepo.findById(resena.getRestaurant().getId())
                .orElseThrow(() -> new RuntimeException("Restaurante no encontrado"));


        boolean yaExiste = resenaRepo.existsByClienteAndRestaurant(cliente, restaurante);
        if (yaExiste) {
            throw new RuntimeException("El cliente ya realizó una reseña para este restaurante.");
        }

        if (resena.getCalificacion() < 1 || resena.getCalificacion() > 5) {
            throw new RuntimeException("La calificación debe estar entre 1 y 5.");
        }

        Resena resenaNueva = new Resena(
                null,
                resena.getComentario(),
                resena.getCalificacion(),
                cliente,
                restaurante
        );
        resenaNueva = resenaRepo.save(resenaNueva);

        return resenaNueva;
    }

    public List<ResenaListDTO> getAllResenas() {
        return resenaRepo.findAll().stream()
                .map(r -> new ResenaListDTO(
                        r.getId(),
                        r.getComentario(),
                        r.getCalificacion()
                )).toList();
    }

    public ResenaDetailDTO getResenaById(Long id) {
        Resena r = resenaRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Reseña no encontrada"));

        return new ResenaDetailDTO(
                r.getId(),
                r.getComentario(),
                r.getCalificacion(),
                r.getCliente().getNombre(),
                r.getRestaurant().getNombre()
        );
    }

    public ResenaDetailDTO updateResena(Long id, ResenaPatchDTO dto) {
        Resena r = resenaRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Reseña no encontrada"));

        if (dto.getComentario() != null) {
            r.setComentario(dto.getComentario());
        }
        if (dto.getCalificacion() != null) {
            int calificacion = dto.getCalificacion();
            if (calificacion < 1 || calificacion > 5) {
                throw new RuntimeException("La calificación debe estar entre 1 y 5.");
            }
            r.setCalificacion(calificacion);
        }

        r = resenaRepo.save(r);

        return new ResenaDetailDTO(
                r.getId(),
                r.getComentario(),
                r.getCalificacion(),
                r.getCliente().getNombre(),
                r.getRestaurant().getNombre()
        );
    }

    public void deleteResena(Long id) {
        if (!resenaRepo.existsById(id)) {
            throw new RuntimeException("La reseña no existe.");
        }
        resenaRepo.deleteById(id);
    }
}