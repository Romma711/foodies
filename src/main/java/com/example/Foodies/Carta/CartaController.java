package com.example.Foodies.Carta;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/carta")
public class CartaController {

    @Autowired
    private CartaService cartaService;

    @GetMapping("/{id}/descargar")
    public ResponseEntity<byte[]> descargarCarta(@PathVariable Long id) {
        byte[] pdf = cartaService.descargarCarta(id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition
                .inline()  // o .attachment()
                .filename("carta.pdf")
                .build());

        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
    }

    @PostMapping("/subir")
    public ResponseEntity<String> subirCarta(
            @RequestParam("archivo") MultipartFile archivo,
            @RequestParam("restaurantId") Long restaurantId
    ) {
        cartaService.guardarCarta(archivo, restaurantId);
        return ResponseEntity.ok("Carta subida correctamente");
    }
}
