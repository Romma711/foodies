package com.example.Foodies.Restaurant;

import com.example.Foodies.Carta.CartaService;
import com.example.Foodies.Carta.Dtos.CartaDTO;
import com.example.Foodies.CupoEspecial.Dtos.CupoEspecialDTO;
import com.example.Foodies.CupoEspecial.Dtos.CupoEspecialRequestDTO;
import com.example.Foodies.Espacio.Dtos.EspacioActivoDTO;
import com.example.Foodies.Espacio.Dtos.EspacioDTO;
import com.example.Foodies.Espacio.Dtos.EspacioRequestDTO;
import com.example.Foodies.Foto.Dtos.FotoDTO;
import com.example.Foodies.Foto.FotoService;
import com.example.Foodies.Reserva.dtos.ReservaPanelDTO;
import com.example.Foodies.Restaurant.Dtos.MiRestauranteDTO;
import com.example.Foodies.Restaurant.Dtos.RestaurantPatchDTO;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
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
    private final FotoService fotoService;

    public RestaurantePanelController(RestaurantePanelService panelService, CartaService cartaService,
                                      FotoService fotoService) {
        this.panelService = panelService;
        this.cartaService = cartaService;
        this.fotoService = fotoService;
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

    /** Espacios del restaurante (activos e inactivos) con su capacidad. */
    @GetMapping("/me/espacios")
    public List<EspacioDTO> getEspacios() {
        return panelService.getEspacios();
    }

    @PostMapping("/me/espacios")
    public ResponseEntity<EspacioDTO> crearEspacio(@RequestBody EspacioRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(panelService.crearEspacio(request));
    }

    @PutMapping("/me/espacios/{id}")
    public EspacioDTO actualizarEspacio(@PathVariable Long id, @RequestBody EspacioRequestDTO request) {
        return panelService.actualizarEspacio(id, request);
    }

    /** Activa o desactiva un espacio. Los espacios no se borran para no perder sus reservas. */
    @PatchMapping("/me/espacios/{id}/activo")
    public EspacioDTO cambiarActivoEspacio(@PathVariable Long id, @RequestBody EspacioActivoDTO request) {
        return panelService.cambiarActivoEspacio(id, request == null ? null : request.activo());
    }

    /** Próximos días con capacidad reducida en los espacios del restaurante del encargado. */
    @GetMapping("/me/cupos-especiales")
    public List<CupoEspecialDTO> getCuposEspeciales() {
        return panelService.getCuposEspeciales();
    }

    /** Crea o modifica el cupo especial de un espacio un día (`yyyy-MM-dd`). */
    @PutMapping("/me/espacios/{espacioId}/cupos-especiales/{fecha}")
    public CupoEspecialDTO guardarCupoEspecial(
            @PathVariable Long espacioId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestBody CupoEspecialRequestDTO request) {
        return panelService.guardarCupoEspecial(espacioId, fecha, request);
    }

    /** Ese día el espacio vuelve a su capacidad normal. */
    @DeleteMapping("/me/espacios/{espacioId}/cupos-especiales/{fecha}")
    public ResponseEntity<Void> eliminarCupoEspecial(
            @PathVariable Long espacioId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        panelService.eliminarCupoEspecial(espacioId, fecha);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me/fotos")
    public List<FotoDTO> getMisFotos() {
        return panelService.getFotos();
    }

    @PostMapping(value = "/me/fotos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public FotoDTO agregarFoto(@RequestParam("archivo") MultipartFile archivo) {
        return panelService.agregarFoto(archivo);
    }

    @PutMapping("/me/fotos/{fotoId}/portada")
    public List<FotoDTO> elegirPortada(@PathVariable Long fotoId) {
        return panelService.elegirPortada(fotoId);
    }

    @PutMapping(value = "/me/fotos/{fotoId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public FotoDTO reemplazarFoto(@PathVariable Long fotoId, @RequestParam("archivo") MultipartFile archivo) {
        return panelService.reemplazarFoto(fotoId, archivo);
    }

    @DeleteMapping("/me/fotos/{fotoId}")
    public ResponseEntity<Void> eliminarFoto(@PathVariable Long fotoId) {
        panelService.eliminarFoto(fotoId);
        return ResponseEntity.noContent().build();
    }

    /** Público: la portada primero y después las demás. */
    @GetMapping("/{id}/fotos")
    public List<FotoDTO> getFotos(@PathVariable Long id) {
        return fotoService.listar(id);
    }

    /** Public: where to download a restaurant's menu. */
    @GetMapping("/{id}/carta")
    public CartaDTO getCarta(@PathVariable Long id) {
        return cartaService.getPorRestauranteId(id);
    }
}
