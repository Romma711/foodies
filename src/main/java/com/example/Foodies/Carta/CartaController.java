package com.example.Foodies.Carta;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

/**
 * Legacy carta routes kept for existing links. Managers upload their menu through
 * `PUT /api/restaurantes/me/carta`; the public read is `GET /api/restaurantes/{id}/carta`.
 */
@RestController
@RequestMapping("api/carta")
public class CartaController {

    private final CartaService cartaService;

    public CartaController(CartaService cartaService) {
        this.cartaService = cartaService;
    }

    /** Redirects to where the PDF is actually stored. */
    @GetMapping("/{id}")
    public ResponseEntity<Void> descargarCarta(@PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(cartaService.urlPorId(id))).build();
    }

    /** Admin only (see SecurityConfig). */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarCarta(@PathVariable Long id) {
        cartaService.eliminarPorId(id);
        return ResponseEntity.noContent().build();
    }
}
