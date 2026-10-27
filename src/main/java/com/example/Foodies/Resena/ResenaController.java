package com.example.Foodies.Resena;

import com.example.Foodies.Resena.dtos.ResenaDetailDTO;
import com.example.Foodies.Resena.dtos.ResenaListDTO;
import com.example.Foodies.Resena.dtos.ResenaPatchDTO;
import com.example.Foodies.Resena.dtos.ResenaRequestDTO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/resenas")
public class ResenaController {

    @Autowired
    private ResenaService resenaService;

    @PostMapping
    public ResponseEntity<ResenaDetailDTO> handleCreateResena(
            @Valid @RequestBody ResenaRequestDTO resena,
            Authentication authentication) {
        ResenaDetailDTO resenaDetailDTO = resenaService.createResena(resena, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(resenaDetailDTO);
    }

    @GetMapping
    public ResponseEntity<List<ResenaListDTO>> handleGetAllResenas(@RequestParam Long id) {
        return ResponseEntity.ok(resenaService.getAllResenasByRestaurant(id));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResenaDetailDTO> handleGetById(@PathVariable Long id) {
        return ResponseEntity.ok(resenaService.getResenaById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ResenaDetailDTO> handleUpdateById(
            @PathVariable Long id,
            @Valid @RequestBody ResenaPatchDTO update,
            Authentication authentication) {
        return ResponseEntity.ok(
                resenaService.updateResena(id, update, authentication.getName(), esAdmin(authentication))
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> handleDeleteById(@PathVariable Long id, Authentication authentication) {
        resenaService.deleteResena(id, authentication.getName(), esAdmin(authentication));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/usuario/{id}")
    public ResponseEntity<?> handlerGetallResenaXUsuario(@PathVariable Long id) {
        return ResponseEntity.ok(resenaService.getallResenaByUsuario(id));
    }

    private boolean esAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }
}
