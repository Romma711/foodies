package com.example.Foodies.Restaurant;

import com.example.Foodies.Enums.EspecialidadDeComida;
import com.example.Foodies.Restaurant.Dtos.RestaurantDetailDTO;
import com.example.Foodies.Restaurant.Dtos.RestaurantListDTO;
import com.example.Foodies.Restaurant.Dtos.RestaurantPatchDTO;
import com.example.Foodies.Restaurant.Dtos.RestaurantRequestDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/restaurantes")
public class RestaurantController {

    @Autowired
    private RestaurantService restaurantService;



    @GetMapping
    public ResponseEntity<?> getAllRestautantes(){
        List<RestaurantListDTO> restaurantListDTOS = restaurantService.getAll();
        return ResponseEntity.ok(restaurantListDTOS);
    }

    @GetMapping("/especialidad")
    public ResponseEntity<?> getEspecialidadRestaurante(@RequestParam EspecialidadDeComida especialidadDeComida){
        List<RestaurantListDTO> restaurantListDTOS = restaurantService.getByEspecialidad(especialidadDeComida);
        return ResponseEntity.ok(restaurantListDTOS);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getByIdRestaurante(@PathVariable Long id){
        RestaurantDetailDTO restaurantDetailDTO = restaurantService.getRestaurantById(id);
        return ResponseEntity.ok(restaurantDetailDTO);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<?> actualizarRestaurante(@PathVariable Long id, @RequestBody RestaurantPatchDTO restaurantPatchDTO){
       RestaurantDetailDTO restaurantDetailDTO = restaurantService.patchRestaurantFromDTO(restaurantPatchDTO,id);
        return ResponseEntity.ok(restaurantDetailDTO);


    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarRestaurante(@PathVariable Long id){
        restaurantService.eliminarRestaurante(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
