package com.example.Foodies.ListaEspera;

import com.example.Foodies.ListaEspera.Dtos.EntradaListaEsperaDTO;
import com.example.Foodies.ListaEspera.Dtos.ListaEsperaRequestDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Waitlist of the authenticated client (RF25). */
@RestController
@RequestMapping("/api/lista-espera")
@Tag(name = "Lista de espera", description = "Lista de espera cuando el turno está lleno (RF25)")
public class ListaEsperaController {

    private final ListaEsperaService listaEsperaService;

    public ListaEsperaController(ListaEsperaService listaEsperaService) {
        this.listaEsperaService = listaEsperaService;
    }

    @Operation(summary = "Anotarse en la lista de espera (cliente)",
            description = "Solo si no hay lugar para esa fecha y horario o turno. Body: idRestaurant, fechaReserva, "
                    + "horarioLlegada, idTurno (opcional), cantidad.")
    @PostMapping
    public ResponseEntity<EntradaListaEsperaDTO> anotar(@RequestBody ListaEsperaRequestDTO pedido) {
        return ResponseEntity.status(HttpStatus.CREATED).body(listaEsperaService.anotar(pedido));
    }

    @Operation(summary = "Mis inscripciones en listas de espera (cliente)")
    @GetMapping("/mias")
    public List<EntradaListaEsperaDTO> mias() {
        return listaEsperaService.mias();
    }

    @Operation(summary = "Salir de una lista de espera (cliente)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> salir(@PathVariable Long id) {
        listaEsperaService.salir(id);
        return ResponseEntity.noContent().build();
    }
}
