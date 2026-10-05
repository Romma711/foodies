package com.example.Foodies.ADMIN;

import com.example.Foodies.Usuario.dtos.UsuarioRestoAAprobarDTO;
import com.example.Foodies.Restaurant.RestaurantService;
import com.example.Foodies.Usuario.UsuarioService;
import com.example.Foodies.Usuario.dtos.UsuarioDetailDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminRestauranteController {

    @Autowired
    private UsuarioService usuarioService;
    @Autowired
    private RestaurantService restaurantService;


    @PutMapping("/approved/{usuarioId}")
    public ResponseEntity<UsuarioDetailDTO> aprobarEncargado(@PathVariable Long usuarioId){
        UsuarioDetailDTO dto = usuarioService.aprobarEncargado(usuarioId);
        return ResponseEntity.ok(dto);
    }

    /** ?page=0&size=20&sort=nombre,asc */
    @GetMapping("/requests")
    public ResponseEntity<Page<UsuarioRestoAAprobarDTO>> getallRestaurantesXAprobar(Pageable pageable){
        return ResponseEntity.ok(restaurantService.getRestaurantNotAprobados(pageable));
    }
}
