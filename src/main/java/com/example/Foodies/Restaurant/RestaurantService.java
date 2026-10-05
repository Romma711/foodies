package com.example.Foodies.Restaurant;



import com.example.Foodies.Enums.EspecialidadDeComida;
import com.example.Foodies.Enums.Role;
import com.example.Foodies.Exception.EntityNotFoundException;
import com.example.Foodies.Exception.HistorialAsociadoException;
import com.example.Foodies.Exception.ListNoContentException;
import com.example.Foodies.Resena.ResenaRepository;
import com.example.Foodies.Reserva.ReservaRepository;
import com.example.Foodies.Restaurant.Dtos.RestaurantDetailDTO;
import com.example.Foodies.Restaurant.Dtos.RestaurantListDTO;
import com.example.Foodies.Restaurant.Dtos.RestaurantPatchDTO;
import com.example.Foodies.Usuario.Usuario;
import com.example.Foodies.Usuario.UsuarioMapper;
import com.example.Foodies.Usuario.UsuarioRepository;
import com.example.Foodies.Usuario.dtos.UsuarioRestoAAprobarDTO;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RestaurantService {

    @Autowired
    private RestaurantRepository restaurantRepo;
    @Autowired
    private ReservaRepository reservaRepo;
    @Autowired
    private ResenaRepository resenaRepo;
    @Autowired
    private UsuarioRepository usuarioRepo;
    @Autowired
    private RestaurantMapper restaurantMapper;
    @Autowired
    private UsuarioMapper usuarioMapper;




    public List<RestaurantListDTO> getByEspecialidad(EspecialidadDeComida especialidadDeComida){
        // Solo se listan restaurantes aprobados (los pendientes solo los ve el dueño o el admin)
        List<Restaurant> restaurants = restaurantRepo.findByEspecialidad(especialidadDeComida).stream()
                .filter(Restaurant::isAprobado)
                .toList();
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

        // Un restaurante pendiente no existe para el publico: solo lo ven su dueño y el admin
        if (!restaurant.isAprobado() && !puedeVerNoAprobado(restaurant)) {
            throw new EntityNotFoundException("restaurant No encontado");
        }

        return restaurantMapper.toDetailDTO(restaurant);
    }

    private boolean puedeVerNoAprobado(Restaurant restaurant) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof String email) || email.isBlank()) {
            return false;
        }
        Usuario usuario = usuarioRepo.findByEmail(email).orElse(null);
        if (usuario == null) {
            return false;
        }
        if (usuario.getRol() == Role.ROLE_ADMIN) {
            return true;
        }
        return restaurant.getUsuario() != null && restaurant.getUsuario().getId().equals(usuario.getId());
    }

    @Transactional
    public RestaurantDetailDTO patchRestaurantFromDTO(RestaurantPatchDTO patchDTO, Long id) {
        Restaurant restaurant = restaurantRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Restaurante no encontrado"));

        verificarAccesoRestaurante(restaurant);

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

    @Transactional
    public void eliminarRestaurante(Long id){
        Restaurant restaurant = restaurantRepo.findById(id).orElseThrow(()-> new EntityNotFoundException("Restaurante no encontrado"));
        verificarAccesoRestaurante(restaurant);

        // Sin esto, el cascade de la entidad borraba reservas y reseñas sin avisar
        if (reservaRepo.existsByRestaurant_Id(id) || resenaRepo.existsByRestaurant_Id(id)) {
            throw new HistorialAsociadoException("El restaurante tiene reservas o reseñas asociadas, no se puede eliminar");
        }

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

    private void verificarAccesoRestaurante(Restaurant restaurant) {
        Usuario usuarioActual = usuarioActual();
        if (isAdmin(usuarioActual)) {
            return;
        }
        if (restaurant.getUsuario() == null || !restaurant.getUsuario().getId().equals(usuarioActual.getId())) {
            throw new AccessDeniedException("No podés modificar un restaurante ajeno");
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

    private boolean isAdmin(Usuario usuario) {
        return usuario.getRol() == Role.ROLE_ADMIN;
    }

}


