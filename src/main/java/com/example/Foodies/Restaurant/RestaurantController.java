package com.example.Foodies.Restaurant;

import com.example.Foodies.Enums.EspecialidadDeComida;
import com.example.Foodies.Restaurant.Dtos.RestaurantDetailDTO;
import com.example.Foodies.Restaurant.Dtos.RestaurantListDTO;
import com.example.Foodies.Restaurant.Dtos.RestaurantPatchDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/restaurantes")
public class RestaurantController {

    @Autowired
    private RestaurantService restaurantService;



    /** ?page=0&size=20&sort=nombre,asc */
    @GetMapping
    public ResponseEntity<Page<RestaurantListDTO>> getAllRestautantes(Pageable pageable){
        return ResponseEntity.ok(restaurantService.getAll(pageable));
    }

    @GetMapping("/especialidad")
    public ResponseEntity<Page<RestaurantListDTO>> getEspecialidadRestaurante(
            @RequestParam EspecialidadDeComida especialidadDeComida, Pageable pageable){
        return ResponseEntity.ok(restaurantService.getByEspecialidad(especialidadDeComida, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getByIdRestaurante(@PathVariable Long id){
        RestaurantDetailDTO restaurantDetailDTO = restaurantService.getRestaurantById(id);
        return ResponseEntity.ok(restaurantDetailDTO);
    }

    @PreAuthorize("hasAnyRole('ENCARGADO', 'ADMIN')")
    @PatchMapping("/{id}")
    public ResponseEntity<?> actualizarRestaurante(@PathVariable Long id, @RequestBody RestaurantPatchDTO restaurantPatchDTO){
       RestaurantDetailDTO restaurantDetailDTO = restaurantService.patchRestaurantFromDTO(restaurantPatchDTO,id);
        return ResponseEntity.ok(restaurantDetailDTO);


    }

    @PreAuthorize("hasAnyRole('ENCARGADO', 'ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarRestaurante(@PathVariable Long id){
        restaurantService.eliminarRestaurante(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
