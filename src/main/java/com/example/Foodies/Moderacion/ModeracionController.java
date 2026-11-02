package com.example.Foodies.Moderacion;

import com.example.Foodies.Moderacion.dtos.EstadoSolicitudDTO;
import com.example.Foodies.Moderacion.dtos.SolicitudRestauranteDTO;
import com.example.Foodies.Moderacion.dtos.StrikeRequestDTO;
import com.example.Foodies.Moderacion.dtos.StrikeResultadoDTO;
import com.example.Foodies.Moderacion.dtos.UbicacionRequestDTO;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoints de moderación de la Fase 3: solicitudes de restaurantes, strikes y ubicación en el mapa.
 * <p>
 * Doble protección para {@code ROLE_ADMIN}: {@code SecurityConfig} ya restringe {@code /api/admin/**}
 * y además {@link PreAuthorize} lo exige a nivel de clase. Sin token responde 401; con otro rol, 403.
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class ModeracionController {

    private final ModeracionService moderacionService;

    public ModeracionController(ModeracionService moderacionService) {
        this.moderacionService = moderacionService;
    }

    /**
     * Solicitudes de alta de restaurantes que esperan revisión.
     *
     * @return 200 con las solicitudes pendientes
     */
    @GetMapping("/restaurantes/pendientes")
    public ResponseEntity<List<SolicitudRestauranteDTO>> pendientes() {
        return ResponseEntity.ok(moderacionService.solicitudesPendientes());
    }

    /**
     * Aprueba una solicitud de alta.
     *
     * @param id id del restaurante
     * @return 200 con el estado APROBADO; 404 si no existe
     */
    @PutMapping("/restaurantes/{id}/aprobar")
    public ResponseEntity<EstadoSolicitudDTO> aprobar(@PathVariable Long id) {
        return ResponseEntity.ok(moderacionService.aprobar(id));
    }

    /**
     * Rechaza una solicitud de alta pendiente.
     *
     * @param id id del restaurante
     * @return 200 con el estado RECHAZADO; 400 si ya estaba aprobado; 404 si no existe
     */
    @PutMapping("/restaurantes/{id}/rechazar")
    public ResponseEntity<EstadoSolicitudDTO> rechazar(@PathVariable Long id) {
        return ResponseEntity.ok(moderacionService.rechazar(id));
    }

    /**
     * Registra un strike sobre un restaurante y avisa por email a su encargado.
     *
     * @param id      id del restaurante
     * @param request motivo de la sanción
     * @return 200 con los strikes acumulados; 400 si falta el motivo; 404 si no existe
     */
    @PostMapping("/restaurantes/{id}/strikes")
    public ResponseEntity<StrikeResultadoDTO> strikeRestaurante(@PathVariable Long id,
                                                                @Valid @RequestBody StrikeRequestDTO request) {
        return ResponseEntity.ok(moderacionService.strikeRestaurante(id, request.motivo()));
    }

    /**
     * Registra un strike sobre un usuario y le avisa por email.
     *
     * @param id      id del usuario
     * @param request motivo de la sanción
     * @return 200 con los strikes acumulados; 400 si falta el motivo o es administrador; 404 si no existe
     */
    @PostMapping("/usuarios/{id}/strikes")
    public ResponseEntity<StrikeResultadoDTO> strikeUsuario(@PathVariable Long id,
                                                            @Valid @RequestBody StrikeRequestDTO request) {
        return ResponseEntity.ok(moderacionService.strikeUsuario(id, request.motivo()));
    }

    /**
     * Carga las coordenadas de un restaurante para el mapa.
     *
     * @param id        id del restaurante
     * @param ubicacion latitud y longitud
     * @return 200 sin cuerpo; 400 si las coordenadas están fuera de rango; 404 si no existe
     */
    @PutMapping("/restaurantes/{id}/ubicacion")
    public ResponseEntity<Void> ubicacion(@PathVariable Long id, @Valid @RequestBody UbicacionRequestDTO ubicacion) {
        moderacionService.actualizarUbicacion(id, ubicacion);
        return ResponseEntity.ok().build();
    }
}
