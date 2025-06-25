package com.example.Foodies.Restaurant;



import com.example.Foodies.Enums.EspecialidadDeComida;
import com.example.Foodies.Exception.EntityNotFoundException;
import com.example.Foodies.Exception.ListNoContentException;
import com.example.Foodies.Restaurant.Dtos.RestaurantDetailDTO;
import com.example.Foodies.Restaurant.Dtos.RestaurantListDTO;
import com.example.Foodies.Restaurant.Dtos.RestaurantPatchDTO;
import com.example.Foodies.Usuario.UsuarioRepository;
import jakarta.transaction.Transactional;
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
    private RestaurantMapper restaurantMapper;



  /*
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
        restaurant.setEspecialidad(especialidad.getTipoDeComida());

        Restaurant saved = restaurantRepo.save(restaurant);

        return restaurantMapper.toDetailDTO(saved);
    }

   */

    public List<RestaurantListDTO> getByEspecialidad(EspecialidadDeComida especialidadDeComida){
        List<Restaurant> restaurants = restaurantRepo.findAll();
        if(restaurants.isEmpty()){
            throw new ListNoContentException("No hay restaurantes con esta especialidad");
        }
        return restaurantMapper.toListDTO(restaurants);
    }

    public List<RestaurantListDTO> getAll() {
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

    @Transactional
    public RestaurantDetailDTO patchRestaurantFromDTO(RestaurantPatchDTO patchDTO, Long id) {
        Restaurant restaurant = restaurantRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Restaurante no encontrado"));

        if (patchDTO.getNombre() != null) {
            restaurant.setNombre(patchDTO.getNombre());
        }
        if (patchDTO.getCupoMaximo() != null) {
            restaurant.setCupoMaximo(patchDTO.getCupoMaximo());
        }
        if (patchDTO.getUbicacion() != null) {
            restaurant.setUbicacion(patchDTO.getUbicacion());
        }
        if (patchDTO.getEspecialidad() != null) {
            restaurant.setEspecialidad(patchDTO.getEspecialidad());
        }

        restaurant = restaurantRepo.save(restaurant);

        return restaurantMapper.toDetailDTO(restaurant);
    }

    public void eliminarRestaurante(Long id){
        Restaurant restaurant = restaurantRepo.findById(id).orElseThrow(()-> new EntityNotFoundException("Restaurante no encontrado"));
        restaurantRepo.delete(restaurant);
    }


}


