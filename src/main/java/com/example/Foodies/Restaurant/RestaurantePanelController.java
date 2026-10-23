package com.example.Foodies.Restaurant;

import com.example.Foodies.Carta.CartaService;
import com.example.Foodies.Carta.Dtos.CartaDTO;
import com.example.Foodies.Reserva.dtos.ReservaPanelDTO;
import com.example.Foodies.Restaurant.Dtos.MiRestauranteDTO;
import com.example.Foodies.Restaurant.Dtos.RestaurantPatchDTO;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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

    /** Public: where to download a restaurant's menu. */
    @GetMapping("/{id}/carta")
    public CartaDTO getCarta(@PathVariable Long id) {
        return cartaService.getPorRestauranteId(id);
    }
}
