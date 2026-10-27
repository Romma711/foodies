package com.example.Foodies.ADMIN.dtos;

import java.util.List;

/**
 * Panel de métricas del administrador, devuelto por {@code GET /api/admin/metricas}.
 *
 * @param periodo                          mes sobre el que se calcula {@code usuariosRegistradosEsteMes},
 *                                         en formato ISO {@code yyyy-MM} (por ejemplo {@code 2026-10})
 * @param topRestaurantesMejorCalificados  hasta 5 restaurantes aprobados con mayor promedio de calificación
 * @param topRestaurantesConMasReservas    hasta 5 restaurantes con más reservas no canceladas
 * @param topUsuariosConMasResenas         hasta 5 usuarios que más reseñas escribieron
 * @param usuariosRegistradosEsteMes       usuarios dados de alta dentro de {@code periodo}
 */
public record MetricasAdminDTO(
        String periodo,
        List<RestauranteCalificacionDTO> topRestaurantesMejorCalificados,
        List<RestauranteReservasDTO> topRestaurantesConMasReservas,
        List<UsuarioResenasDTO> topUsuariosConMasResenas,
        long usuariosRegistradosEsteMes
) {
}
