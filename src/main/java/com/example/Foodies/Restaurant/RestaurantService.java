package com.example.Foodies.Restaurant;



import com.example.Foodies.Enums.EspecialidadDeComida;
import com.example.Foodies.Enums.Role;
import com.example.Foodies.Exception.EntityNotFoundException;
import com.example.Foodies.Exception.ListNoContentException;
import com.example.Foodies.Restaurant.Dtos.RestaurantDetailDTO;
import com.example.Foodies.Restaurant.Dtos.RestaurantListDTO;
import com.example.Foodies.Restaurant.Dtos.RestaurantPatchDTO;
import com.example.Foodies.Usuario.Usuario;
import com.example.Foodies.Usuario.UsuarioMapper;
import com.example.Foodies.Usuario.UsuarioRepository;
import com.example.Foodies.Usuario.dtos.UsuarioDetailDTO;
import com.example.Foodies.Usuario.dtos.UsuarioRestoAAprobarDTO;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RestaurantService {

    @Autowired
    private RestaurantRepository restaurantRepo;
    @Autowired
    private UsuarioRepository usuarioRepo;
    @Autowired
    private RestaurantMapper restaurantMapper;
    @Autowired
    private UsuarioMapper usuarioMapper;




    public List<RestaurantListDTO> getByEspecialidad(EspecialidadDeComida especialidadDeComida){
        List<Restaurant> restaurants = restaurantRepo.findByEspecialidad(especialidadDeComida);
        if(restaurants.isEmpty()){
            throw new ListNoContentException("No hay restaurantes con esta especialidad");
        }
        return restaurantMapper.toListDTO(restaurants);
    }

    public List<RestaurantListDTO> getAll() {
        List<Restaurant> restaurantes = restaurantRepo.findByAprobadoTrue();
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

    public List<UsuarioRestoAAprobarDTO> getRestaurantNotAprobados(){
        List<UsuarioRestoAAprobarDTO> usuarios = restaurantRepo.findByAprobadoFalse().stream()
                .map(r -> new UsuarioRestoAAprobarDTO(
                        r.getUsuario().getId(),
                        r.getNombre(),
                        r.getUsuario().getEmail(),
                        r.getUsuario().getTelefono(),
                        String.valueOf(r.getUsuario().getRol())
                ))
                .toList();
        if(usuarios.isEmpty()){
            throw new ListNoContentException("no hay ningun restaurant para aprobar");
        }
        return usuarios;
    }

}


