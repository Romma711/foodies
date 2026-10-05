package com.example.Foodies.Resena;

import com.example.Foodies.Resena.dtos.ResenaDetailDTO;
import com.example.Foodies.Resena.dtos.ResenaListDTO;
import com.example.Foodies.Resena.dtos.ResenaPatchDTO;
import com.example.Foodies.Resena.dtos.ResenaRequestDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/resenas")
public class ResenaController {

    @Autowired
    private ResenaService resenaService;

    @PreAuthorize("hasRole('CLIENTE')")
    @PostMapping
    public ResponseEntity<ResenaDetailDTO> handleCreateResena(@RequestBody ResenaRequestDTO resena) {
        ResenaDetailDTO resenaDetailDTO = resenaService.createResena(resena);
        return ResponseEntity.status(HttpStatus.CREATED).body(resenaDetailDTO);
    }

    @PreAuthorize("hasAnyRole('CLIENTE', 'ENCARGADO', 'ADMIN')")
    @GetMapping
    public ResponseEntity<Page<ResenaListDTO>> handleGetAllResenas(@RequestParam Long id, Pageable pageable) {
        return ResponseEntity.ok(resenaService.getAllResenasByRestaurant(id, pageable));
    }

    @PreAuthorize("hasAnyRole('CLIENTE', 'ENCARGADO', 'ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<ResenaDetailDTO> handleGetById(@PathVariable Long id) {
        return ResponseEntity.ok(resenaService.getResenaById(id));
    }

    @PreAuthorize("hasAnyRole('CLIENTE', 'ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<ResenaDetailDTO> handleUpdateById(
            @PathVariable Long id,
            @RequestBody ResenaPatchDTO update) {
        return ResponseEntity.ok(resenaService.updateResena(id, update));
    }

    @PreAuthorize("hasAnyRole('CLIENTE', 'ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> handleDeleteById(@PathVariable Long id) {
        resenaService.deleteResena(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('CLIENTE', 'ADMIN')")
    @GetMapping("/usuario/{id}")
    public ResponseEntity<Page<ResenaListDTO>> handlerGetallResenaXUsuario (@PathVariable Long id, Pageable pageable){
        return ResponseEntity.ok(resenaService.getallResenaByUsuario(id, pageable));
    }
}