package com.example.Foodies.Restaurant;

import com.example.Foodies.EspecialidadDeComida.EspecialidadDeComida;
import com.example.Foodies.EspecialidadDeComida.EspecialidadRepository;
import com.example.Foodies.Exception.BusinessException;
import com.example.Foodies.Exception.EntityNotFoundException;
import com.example.Foodies.Exception.ListNoContentException;
import com.example.Foodies.Restaurant.Dtos.RestaurantDetailDTO;
import com.example.Foodies.Restaurant.Dtos.RestaurantListDTO;
import com.example.Foodies.Restaurant.Dtos.RestaurantPatchDTO;
import com.example.Foodies.Restaurant.Dtos.RestaurantRequestDTO;
import com.example.Foodies.Usuario.Usuario;
import com.example.Foodies.Usuario.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RestaurantService {

    @Autowired
    private RestaurantRepository restaurantRepo;
    @Autowired
    private UsuarioRepository usuarioRepo;
    @Autowired
    private EspecialidadRepository especialidadRepo;
    @Autowired
    private RestaurantMapper restaurantMapper;




    public RestaurantDetailDTO crearRestaurant(RestaurantRequestDTO dto) {

        // Buscar usuario
        Usuario usuario = usuarioRepo.findById(dto.getUsuarioId())
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));

        // Validar que no tenga ya un restaurante
        if (restaurantRepo.existByUsuarioId(usuario.getId())) {
            throw new BusinessException("Este usuario ya tiene un restaurante");
        }

        // Buscar especialidad
        EspecialidadDeComida especialidad = especialidadRepo.findById(dto.getEspecialidadId())
                .orElseThrow(() -> new EntityNotFoundException("Especialidad no encontrada"));

        // Crear restaurante
        Restaurant restaurant = restaurantMapper.toEntity(dto);
        restaurant.setUsuario(usuario);
        restaurant.setEspecialidad(especialidad);

        Restaurant saved = restaurantRepo.save(restaurant);

        return restaurantMapper.toDetailDTO(saved);
    }

    public List<RestaurantListDTO> listarTodos() {
        List<Restaurant> restaurantes = restaurantRepo.findAll();
        if(restaurantes.isEmpty()){
            throw new ListNoContentException("No hay restaurantes");
        }

        return restaurantMapper.toListDTO(restaurantes);
    }

    public RestaurantDetailDTO getRestaurantById(Long restaurantId){
        Restaurant restaurant = restaurantRepo.findById(restaurantId).orElseThrow(()-> new EntityNotFoundException("restaurant No encontado"));
       return restaurantMapper.toDetailDTO(restaurant);
    }


    public RestaurantDetailDTO patchRestaurant(Long restaurantId, RestaurantPatchDTO patchDTO) {
        // 1. Buscar el restaurante existente
        Restaurant restaurant = restaurantRepo.findById(restaurantId)
                .orElseThrow(() -> new EntityNotFoundException("Restaurante no encontrado"));

        // 2. Mapear los campos que vienen en patchDTO al entity (sin sobrescribir campos null)
        restaurantMapper.patchRestaurantFromDTO(patchDTO, restaurant);

        // 3. Guardar el restaurante modificado
        restaurant = restaurantRepo.save(restaurant);

        // 4. Mapear a DTO de detalle y devolver
        return restaurantMapper.toDetailDTO(restaurant);
    }



}


