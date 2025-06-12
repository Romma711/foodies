package com.example.Foodies.Resena;

import com.example.Foodies.Cliente.Cliente;
import com.example.Foodies.Cliente.ClienteRepository;
import com.example.Foodies.Restaurant.Restaurant;
import com.example.Foodies.Restaurant.RestaurantRepository;
import com.example.Foodies.Resena.dtos.ResenaDetailDTO;
import com.example.Foodies.Resena.dtos.ResenaListDTO;
import com.example.Foodies.Resena.dtos.ResenaPatchDTO;
import com.example.Foodies.Resena.dtos.ResenaRequestDTO;
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

    public ResenaDetailDTO createResena(ResenaRequestDTO dto) {
        Cliente cliente = clienteRepo.findById(dto.getId())
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

        Restaurant restaurante = restauranteRepo.findById(dto.getId())
                .orElseThrow(() -> new RuntimeException("Restaurante no encontrado"));

        Resena resena = new Resena(
                dto.getComentario(),
                dto.getCalificacion(),
                cliente,
                restaurante
        );
        resena = resenaRepo.save(resena);

        return new ResenaDetailDTO(
                resena.getId(),
                resena.getComentario(),
                resena.getCalificacion(),
                cliente.getNombre(),
                restaurante.getNombre()
        );
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
        if (dto.getCalificacion() < 6 && dto.getCalificacion() > 0) {
            r.setCalificacion(dto.getCalificacion());
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
        resenaRepo.deleteById(id);
    }
}