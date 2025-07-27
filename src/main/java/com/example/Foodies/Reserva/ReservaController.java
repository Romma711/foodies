package com.example.Foodies.Reserva;

import com.example.Foodies.Reserva.dtos.ReservaDetailDTO;
import com.example.Foodies.Reserva.dtos.ReservaListDTO;
import com.example.Foodies.Reserva.dtos.ReservaPatchDTO;
import com.example.Foodies.Reserva.dtos.ReservaRequesDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reservas")
public class ReservaController {

    @Autowired
    private ReservaService reservaService;

    @PostMapping("/")
    public ResponseEntity<ReservaDetailDTO> handleCreateReserva(@RequestBody ReservaRequesDTO reserva) {
        return ResponseEntity.ok(reservaService.createReserva(reserva));
    }

    @GetMapping("/")
    public ResponseEntity<List<ReservaListDTO>> handleGetAllReservas() {
        return ResponseEntity.ok(reservaService.getAllReservas());
    }

    @GetMapping("/cliente/{id}")
    public ResponseEntity<List<ReservaListDTO>> handleGetAllReservasByCliente(@PathVariable Long id) {
        return ResponseEntity.ok(reservaService.getAllByCliente(id));
    }

    @GetMapping("/restaurante/{id}")
    public ResponseEntity<List<ReservaListDTO>> handleGetAllReservasByRestaurant(@PathVariable Long id) {
        return ResponseEntity.ok(reservaService.getAllByRestaurant(id));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReservaDetailDTO> handleGetById(@PathVariable Long id) {
        return ResponseEntity.ok(reservaService.getReservaById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ReservaDetailDTO> handleUpdateById(
            @PathVariable Long id,
            @RequestBody ReservaPatchDTO update) {
        return ResponseEntity.ok(reservaService.updateReserva(id, update));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> handleDeleteById(@PathVariable Long id) {
        reservaService.deleteReserva(id);
        return ResponseEntity.noContent().build();
    }
}