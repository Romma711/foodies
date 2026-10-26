package com.example.Foodies.ADMIN;

import com.example.Foodies.ADMIN.dtos.MetricasAdminDTO;
import com.example.Foodies.Enums.EstadoReserva;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;

/**
 * Reúne las métricas del panel de administración.
 * <p>
 * Solo lee datos: delega cada cálculo en {@link AdminMetricasRepository} y arma un único
 * {@link MetricasAdminDTO} para que el frontend resuelva el dashboard con una sola llamada.
 */
@Service
public class AdminService {

    /** Zona horaria del negocio: "este mes" se calcula según el calendario de Argentina. */
    static final ZoneId ZONA_NEGOCIO = ZoneId.of("America/Argentina/Buenos_Aires");

    /** Tamaño de cada ranking del panel. */
    private static final Pageable TOP_5 = PageRequest.of(0, 5);

    private final AdminMetricasRepository metricasRepo;
    private final Clock clock;

    @Autowired
    public AdminService(AdminMetricasRepository metricasRepo) {
        this(metricasRepo, Clock.system(ZONA_NEGOCIO));
    }

    /** Permite fijar la fecha actual en los tests. */
    AdminService(AdminMetricasRepository metricasRepo, Clock clock) {
        this.metricasRepo = metricasRepo;
        this.clock = clock;
    }

    /**
     * Calcula todas las métricas del panel.
     * <ul>
     *   <li>Top 5 restaurantes aprobados con mejor promedio de calificación.</li>
     *   <li>Top 5 restaurantes con más reservas, sin contar las canceladas.</li>
     *   <li>Top 5 usuarios con más reseñas escritas.</li>
     *   <li>Usuarios registrados desde el día 1 del mes en curso hasta el día 1 del mes siguiente (exclusivo).</li>
     * </ul>
     *
     * @return métricas listas para el dashboard
     */
    @Transactional(readOnly = true)
    public MetricasAdminDTO obtenerMetricas() {
        YearMonth mesActual = YearMonth.now(clock);
        LocalDateTime inicioMes = mesActual.atDay(1).atStartOfDay();
        LocalDateTime inicioMesSiguiente = mesActual.plusMonths(1).atDay(1).atStartOfDay();

        return new MetricasAdminDTO(
                mesActual.toString(),
                metricasRepo.topRestaurantesMejorCalificados(TOP_5),
                metricasRepo.topRestaurantesConMasReservas(EstadoReserva.CANCELADA, TOP_5),
                metricasRepo.topUsuariosConMasResenas(TOP_5),
                metricasRepo.contarUsuariosRegistradosEntre(inicioMes, inicioMesSiguiente));
    }
}
