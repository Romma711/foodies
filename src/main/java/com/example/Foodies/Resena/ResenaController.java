package com.example.Foodies.Resena;

import com.example.Foodies.Resena.dtos.ResenaDetailDTO;
import com.example.Foodies.Resena.dtos.ResenaListDTO;
import com.example.Foodies.Resena.dtos.ResenaPatchDTO;
import com.example.Foodies.Resena.dtos.ResenaRequestDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/resenas")
public class ResenaController {

    @Autowired
    private ResenaService resenaService;

    @PostMapping
    public ResponseEntity<ResenaDetailDTO> handleCreateResena(@RequestBody ResenaRequestDTO resena) {
        ResenaDetailDTO resenaDetailDTO = resenaService.createResena(resena);
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
            @RequestBody ResenaPatchDTO update) {
        return ResponseEntity.ok(resenaService.updateResena(id, update));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> handleDeleteById(@PathVariable Long id) {
        resenaService.deleteResena(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/cliente/{id}")
    public ResponseEntity<?> handlerGetallResenaXcliente (@PathVariable Long id){
        return ResponseEntity.ok(resenaService.getallResenaByIdCliente(id));
    }
}