package com.example.Foodies.Resena;

import com.example.Foodies.Cliente.Cliente;
import com.example.Foodies.Cliente.ClienteRepository;
import com.example.Foodies.Exception.ListNoContentException;
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
    @Autowired
    private  ResenaMapper resenaMapper;

    @Transactional
    public ResenaDetailDTO createResena(ResenaRequestDTO resena) {
        Cliente cliente = clienteRepo.findById(resena.getClienteId())
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

        Restaurant restaurante = restauranteRepo.findById(resena.getRestaurantId())
                .orElseThrow(() -> new RuntimeException("Restaurante no encontrado"));

        boolean yaExiste = resenaRepo.existsByCliente_IdAndRestaurant_Id(resena.getClienteId(), resena.getRestaurantId());
        if (yaExiste) {
            throw new RuntimeException("El cliente ya realizó una reseña para este restaurante.");
        }

        if (resena.getCalificacion() < 1 || resena.getCalificacion() > 5) {
            throw new RuntimeException("La calificación debe estar entre 1 y 5.");
        }

        Resena resenaNueva = resenaMapper.toEntity(resena);
        resenaNueva.setCliente(cliente);
        resenaNueva.setRestaurant(restaurante);

        resenaNueva = resenaRepo.save(resenaNueva);

        return resenaMapper.toDto(resenaNueva);
    }

    public List<ResenaListDTO> getAllResenasByRestaurant(Long id) {
        List<Resena> resenaList = resenaRepo.findByRestaurant_Id(id);
        if(resenaList.isEmpty()){
            throw new ListNoContentException("Este restaurante no tiene resenas");
        }
       return resenaMapper.toListDtoList(resenaList);

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

    public List<ResenaListDTO> getallResenaByIdCliente (Long id){
        List<Resena> resenas = resenaRepo.findByCliente_Id(id);
        if(resenas.isEmpty()){
            throw new ListNoContentException("Este cliente no tiene resenas");
        }
        return resenaMapper.toListDtoList(resenas);

    }
}