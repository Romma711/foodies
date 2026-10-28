package com.example.Foodies.Moderacion;

import com.example.Foodies.Moderacion.dtos.ActivoRequestDTO;
import com.example.Foodies.Moderacion.dtos.EntidadClienteDTO;
import com.example.Foodies.Moderacion.dtos.EntidadRestauranteDTO;
import com.example.Foodies.Moderacion.dtos.EstadoActivoDTO;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoints de "Gestión de Entidades" (Fase 3): listados para el panel y baja lógica.
 * <p>
 * Doble protección para {@code ROLE_ADMIN}: {@code SecurityConfig} ya restringe {@code /api/admin/**}
 * y además {@link PreAuthorize} lo exige a nivel de clase. Sin token responde 401; con otro rol, 403.
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class EntidadesController {

    private final EntidadesService entidadesService;

    public EntidadesController(EntidadesService entidadesService) {
        this.entidadesService = entidadesService;
    }

    /**
     * @return 200 con los clientes, sus strikes y si están activos
     */
    @GetMapping("/entidades/clientes")
    public ResponseEntity<List<EntidadClienteDTO>> clientes() {
        return ResponseEntity.ok(entidadesService.clientes());
    }

    /**
     * @return 200 con todos los restaurantes, su estado, strikes y si están activos
     */
    @GetMapping("/entidades/restaurantes")
    public ResponseEntity<List<EntidadRestauranteDTO>> restaurantes() {
        return ResponseEntity.ok(entidadesService.restaurantes());
    }

    /**
     * Da de baja o reactiva una cuenta. Una cuenta dada de baja no puede loguearse ni usar la API.
     *
     * @param id      id del usuario
     * @param request nuevo valor de {@code activo}
     * @return 200 con el estado final; 400 sin valor o si es administrador; 404 si no existe
     */
    @PatchMapping("/usuarios/{id}/activo")
    public ResponseEntity<EstadoActivoDTO> activoUsuario(@PathVariable Long id,
                                                         @Valid @RequestBody ActivoRequestDTO request) {
        return ResponseEntity.ok(entidadesService.cambiarActivoUsuario(id, request.activo()));
    }

    /**
     * Da de baja o reactiva un restaurante. Dado de baja no aparece en el catálogo público.
     *
     * @param id      id del restaurante
     * @param request nuevo valor de {@code activo}
     * @return 200 con el estado final; 400 sin valor; 404 si no existe
     */
    @PatchMapping("/restaurantes/{id}/activo")
    public ResponseEntity<EstadoActivoDTO> activoRestaurante(@PathVariable Long id,
                                                             @Valid @RequestBody ActivoRequestDTO request) {
        return ResponseEntity.ok(entidadesService.cambiarActivoRestaurante(id, request.activo()));
    }
}
