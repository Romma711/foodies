package com.example.Foodies.Reserva;

import com.example.Foodies.Reserva.dtos.ReservaDetailDTO;
import com.example.Foodies.Reserva.dtos.ReservaListDTO;
import com.example.Foodies.Reserva.dtos.ReservaPatchDTO;
import com.example.Foodies.Reserva.dtos.ReservaRequesDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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

    /** Listados paginados: ?page=0&size=20&sort=cantidad,asc */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<Page<ReservaListDTO>> handleGetAllReservas(Pageable pageable) {
        return ResponseEntity.ok(reservaService.getAllReservas(pageable));
    }

    @PreAuthorize("hasAnyRole('CLIENTE', 'ADMIN')")
    @GetMapping("/usuario/{id}")
    public ResponseEntity<Page<ReservaListDTO>> handleGetAllReservasByUsuario(
            @PathVariable Long id, Pageable pageable) {
        return ResponseEntity.ok(reservaService.getAllByUsuario(id, pageable));
    }

    @PreAuthorize("hasAnyRole('ENCARGADO', 'ADMIN')")
    @GetMapping("/restaurante/{id}")
    public ResponseEntity<Page<ReservaListDTO>> handleGetAllReservasByRestaurant(
            @PathVariable Long id, Pageable pageable) {
        return ResponseEntity.ok(reservaService.getAllByRestaurant(id, pageable));
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