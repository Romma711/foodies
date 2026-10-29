package com.example.Foodies.Restaurant;

import com.example.Foodies.Carta.CartaService;
import com.example.Foodies.Carta.Dtos.CartaDTO;
import com.example.Foodies.CupoEspecial.Dtos.CupoEspecialDTO;
import com.example.Foodies.CupoEspecial.Dtos.CupoEspecialRequestDTO;
import com.example.Foodies.Reserva.dtos.ReservaPanelDTO;
import com.example.Foodies.Restaurant.Dtos.MiRestauranteDTO;
import com.example.Foodies.Restaurant.Dtos.RestaurantPatchDTO;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

/** Restaurant manager panel. `me` is always the restaurant of the authenticated ROLE_ENCARGADO user. */
@RestController
@RequestMapping("/api/restaurantes")
public class RestaurantePanelController {

    private final RestaurantePanelService panelService;
    private final CartaService cartaService;

    public RestaurantePanelController(RestaurantePanelService panelService, CartaService cartaService) {
        this.panelService = panelService;
        this.cartaService = cartaService;
    }

    @GetMapping("/me")
    public MiRestauranteDTO getMiRestaurante() {
        return panelService.getMiRestaurante();
    }

    @PatchMapping("/me")
    public MiRestauranteDTO actualizar(@RequestBody RestaurantPatchDTO patch) {
        return panelService.actualizar(patch);
    }

    @PutMapping(value = "/me/carta", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CartaDTO guardarCarta(@RequestParam("archivo") MultipartFile archivo) {
        return panelService.guardarCarta(archivo);
    }

    @DeleteMapping("/me/carta")
    public ResponseEntity<Void> eliminarCarta() {
        panelService.eliminarCarta();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me/reservas")
    public List<ReservaPanelDTO> getReservas() {
        return panelService.getReservas();
    }

    /** Próximos días con capacidad reducida del restaurante del encargado. */
    @GetMapping("/me/cupos-especiales")
    public List<CupoEspecialDTO> getCuposEspeciales() {
        return panelService.getCuposEspeciales();
    }

    /** Crea o modifica el cupo especial de un día (`yyyy-MM-dd`). */
    @PutMapping("/me/cupos-especiales/{fecha}")
    public CupoEspecialDTO guardarCupoEspecial(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestBody CupoEspecialRequestDTO request) {
        return panelService.guardarCupoEspecial(fecha, request);
    }

    /** Ese día vuelve a regir la capacidad normal. */
    @DeleteMapping("/me/cupos-especiales/{fecha}")
    public ResponseEntity<Void> eliminarCupoEspecial(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        panelService.eliminarCupoEspecial(fecha);
        return ResponseEntity.noContent().build();
    }

    /** Public: where to download a restaurant's menu. */
    @GetMapping("/{id}/carta")
    public CartaDTO getCarta(@PathVariable Long id) {
        return cartaService.getPorRestauranteId(id);
    }
}
