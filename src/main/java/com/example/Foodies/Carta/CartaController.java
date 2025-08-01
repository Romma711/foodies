package com.example.Foodies.Carta;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("api/carta")
public class CartaController {

    @Autowired
    private CartaService cartaService;

    @GetMapping("/{id}")
    public ResponseEntity<byte[]> descargarCarta(@PathVariable Long id) {
        Carta carta = cartaService.descargarCarta(id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition
                .inline()  // o .attachment()
                .filename(carta.getNombreArchivo())
                .build());

        return new ResponseEntity<>(carta.getContenidoPdf(), headers, HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<String> subirCarta(
            @RequestParam("archivo") MultipartFile archivo,
            @RequestParam("restaurantId") Long restaurantId
    ) {
        cartaService.guardarCarta(archivo, restaurantId);
        return ResponseEntity.ok("Carta subida correctamente");
    }

    @PutMapping
    public ResponseEntity<String> actualizarCarta(
            @RequestParam("archivo") MultipartFile archivo,
            @RequestParam("restaurantId") Long restaurantId
    ) {
        cartaService.actualizarCarta(archivo, restaurantId);
        return ResponseEntity.ok("Carta actualizada correctamente");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarCarta(@PathVariable Long id) {
        cartaService.eliminarCarta(id);
        return ResponseEntity.noContent().build();
    }






}
