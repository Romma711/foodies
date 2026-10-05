package com.example.Foodies.Reserva;

import com.example.Foodies.Reserva.dtos.ReservaDetailDTO;
import com.example.Foodies.Reserva.dtos.ReservaListDTO;
import com.example.Foodies.Reserva.dtos.ReservaPatchDTO;
import com.example.Foodies.Reserva.dtos.ReservaRequesDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reservas")
public class ReservaController {

    @Autowired
    private ReservaService reservaService;

    @PreAuthorize("hasAnyRole('CLIENTE', 'ADMIN')")
    @PostMapping
    public ResponseEntity<ReservaDetailDTO> handleCreateReserva(@RequestBody ReservaRequesDTO reserva) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reservaService.createReserva(reserva));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<ReservaListDTO>> handleGetAllReservas() {
        return ResponseEntity.ok(reservaService.getAllReservas());
    }

    @PreAuthorize("hasAnyRole('CLIENTE', 'ADMIN')")
    @GetMapping("/usuario/{id}")
    public ResponseEntity<List<ReservaListDTO>> handleGetAllReservasByUsuario(@PathVariable Long id) {
        return ResponseEntity.ok(reservaService.getAllByUsuario(id));
    }

    @PreAuthorize("hasAnyRole('ENCARGADO', 'ADMIN')")
    @GetMapping("/restaurante/{id}")
    public ResponseEntity<List<ReservaListDTO>> handleGetAllReservasByRestaurant(@PathVariable Long id) {
        return ResponseEntity.ok(reservaService.getAllByRestaurant(id));
    }

    @PreAuthorize("hasAnyRole('CLIENTE', 'ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<ReservaDetailDTO> handleGetById(@PathVariable Long id) {
        return ResponseEntity.ok(reservaService.getReservaById(id));
    }

    @PreAuthorize("hasAnyRole('CLIENTE', 'ENCARGADO', 'ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<ReservaDetailDTO> handleUpdateById(
            @PathVariable Long id,
            @RequestBody ReservaPatchDTO update) {
        return ResponseEntity.ok(reservaService.updateReserva(id, update));
    }

    @PreAuthorize("hasAnyRole('CLIENTE', 'ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> handleDeleteById(@PathVariable Long id) {
        reservaService.deleteReserva(id);
        return ResponseEntity.noContent().build();
    }
}