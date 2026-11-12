package com.example.Foodies.Espacio;

import com.example.Foodies.Espacio.Dtos.EspacioPublicoDTO;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/** Listado público de los espacios donde se puede reservar en un restaurante. */
@RestController
@RequestMapping("/api/restaurantes/{restaurantId}/espacios")
public class EspacioController {

    private final EspacioService espacioService;

    public EspacioController(EspacioService espacioService) {
        this.espacioService = espacioService;
    }

    /**
     * Espacios activos del restaurante. Con {@code fecha} ({@code yyyy-MM-dd}) y {@code cantidad}
     * indica en cada uno si hay lugar; los dos parámetros van juntos.
     */
    @GetMapping
    public List<EspacioPublicoDTO> listar(@PathVariable Long restaurantId,
                                          @RequestParam(required = false)
                                          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                                          @RequestParam(required = false) Integer cantidad) {
        return espacioService.listarPublicos(restaurantId, fecha, cantidad);
    }
}
