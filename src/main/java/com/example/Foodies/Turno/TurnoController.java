package com.example.Foodies.Turno;

import com.example.Foodies.Config.CurrentUser;
import com.example.Foodies.Exception.EntityNotFoundException;
import com.example.Foodies.Restaurant.Restaurant;
import com.example.Foodies.Restaurant.RestaurantRepository;
import com.example.Foodies.Turno.Dtos.DisponibilidadDTO;
import com.example.Foodies.Turno.Dtos.TurnoDTO;
import com.example.Foodies.Turno.Dtos.TurnoRequestDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/** Shifts (RF24): the manager edits them from the panel; the public lists them and asks for availability. */
@RestController
@RequestMapping("/api/restaurantes")
@Tag(name = "Turnos", description = "Turnos con cupo por franja horaria (RF24)")
public class TurnoController {

    private final TurnoService turnoService;
    private final RestaurantRepository restaurantRepo;
    private final CurrentUser currentUser;

    public TurnoController(TurnoService turnoService, RestaurantRepository restaurantRepo, CurrentUser currentUser) {
        this.turnoService = turnoService;
        this.restaurantRepo = restaurantRepo;
        this.currentUser = currentUser;
    }

    @Operation(summary = "Turnos de mi restaurante (encargado)")
    @GetMapping("/me/turnos")
    public List<TurnoDTO> getMisTurnos() {
        return turnoService.listar(miRestaurante());
    }

    @Operation(summary = "Crear un turno (encargado)", description = "Body: dia, inicio HH:mm, duracionMinutos (90 por defecto).")
    @PostMapping("/me/turnos")
    public ResponseEntity<TurnoDTO> crear(@RequestBody TurnoRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(turnoService.crear(miRestaurante(), request));
    }

    @Operation(summary = "Modificar un turno sin reservas futuras (encargado)")
    @PutMapping("/me/turnos/{id}")
    public TurnoDTO actualizar(@PathVariable Long id, @RequestBody TurnoRequestDTO request) {
        return turnoService.actualizar(miRestaurante(), id, request);
    }

    @Operation(summary = "Quitar un turno sin reservas futuras (encargado)")
    @DeleteMapping("/me/turnos/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        turnoService.eliminar(miRestaurante(), id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Turnos de un restaurante (público)")
    @GetMapping("/{id}/turnos")
    public List<TurnoDTO> getTurnos(@PathVariable Long id) {
        return turnoService.listarPublico(id);
    }

    @Operation(summary = "Lugares libres por turno y espacio en una fecha (público)",
            description = "Sin turnos cargados devuelve un único bloque del día con idTurno null.")
    @GetMapping("/{id}/disponibilidad")
    public DisponibilidadDTO disponibilidad(@PathVariable Long id,
                                            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return turnoService.disponibilidad(id, fecha);
    }

    private Restaurant miRestaurante() {
        return restaurantRepo.findByUsuario_Email(currentUser.email())
                .orElseThrow(() -> new EntityNotFoundException("No tenés un restaurante asociado"));
    }
}
