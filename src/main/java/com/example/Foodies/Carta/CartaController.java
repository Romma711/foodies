package com.example.Foodies.Carta;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("api/carta")
public class CartaController {

    @Autowired
    private CartaService cartaService;

    @GetMapping("/{restaurantId}")
    public ResponseEntity<byte[]> descargarCarta(@PathVariable Long restaurantId) {
        Carta carta = cartaService.descargarCarta(restaurantId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition
                .inline()  // o .attachment()
                .filename(carta.getNombreArchivo())
                .build());

        return new ResponseEntity<>(carta.getContenidoPdf(), headers, HttpStatus.OK);
    }

    @PreAuthorize("hasAnyRole('ENCARGADO', 'ADMIN')")
    @PostMapping
    public ResponseEntity<String> subirCarta(
            @RequestParam("archivo") MultipartFile archivo,
            @RequestParam("restaurantId") Long restaurantId
    ) {
        cartaService.guardarCarta(archivo, restaurantId);
        return ResponseEntity.ok("Carta subida correctamente");
    }

    @PreAuthorize("hasAnyRole('ENCARGADO', 'ADMIN')")
    @PutMapping
    public ResponseEntity<String> actualizarCarta(
            @RequestParam("archivo") MultipartFile archivo,
            @RequestParam("restaurantId") Long restaurantId
    ) {
        cartaService.actualizarCarta(archivo, restaurantId);
        return ResponseEntity.ok("Carta actualizada correctamente");
    }

    /**
     * Borra la carta del restaurante. Usa el restaurantId (igual que el GET) y no el
     * id de la carta, para no tener dos ids distintos en la misma ruta.
     */
    @PreAuthorize("hasAnyRole('ENCARGADO', 'ADMIN')")
    @DeleteMapping("/{restaurantId}")
    public ResponseEntity<Void> eliminarCarta(@PathVariable Long restaurantId) {
        cartaService.eliminarCarta(restaurantId);
        return ResponseEntity.noContent().build();
    }
}