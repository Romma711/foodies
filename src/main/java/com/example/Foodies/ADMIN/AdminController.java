package com.example.Foodies.ADMIN;

import com.example.Foodies.ADMIN.dtos.MetricasAdminDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints del panel de administración (dashboard).
 * <p>
 * Doble protección para {@code ROLE_ADMIN}: {@code SecurityConfig} ya restringe {@code /api/admin/**}
 * y además {@link PreAuthorize} lo exige a nivel de método, por si la regla de URL cambiara.
 * Sin token responde 401; con un token de otro rol responde 403.
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    /**
     * Devuelve las métricas del dashboard: rankings de restaurantes y usuarios y altas del mes.
     *
     * @return 200 con {@link MetricasAdminDTO}
     */
    @GetMapping("/metricas")
    public ResponseEntity<MetricasAdminDTO> obtenerMetricas() {
        return ResponseEntity.ok(adminService.obtenerMetricas());
    }
}
