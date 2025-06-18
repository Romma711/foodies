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
@RequestMapping("/reserva")
public class ReservaController {

    @Autowired
    private ReservaService reservaService;

    @PostMapping("/create")
    public ResponseEntity<Reserva> handleCreateReserva(@RequestBody Reserva reserva) {
        return ResponseEntity.ok(reservaService.createReserva(reserva));
    }

    @GetMapping("/list")
    public ResponseEntity<List<ReservaListDTO>> handleGetAllReservas() {
        return ResponseEntity.ok(reservaService.getAllReservas());
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