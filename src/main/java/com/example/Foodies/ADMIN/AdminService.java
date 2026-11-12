package com.example.Foodies.ADMIN;

import com.example.Foodies.Exception.BusinessException;
import com.example.Foodies.ADMIN.dtos.MetricasAdminDTO;
import com.example.Foodies.ADMIN.dtos.ResenaModeracionDTO;
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
import java.util.List;

/**
 * Lógica de lectura del panel de administración: métricas del dashboard y reseñas para moderar.
 * <p>
 * Solo lee datos: delega las métricas en {@link AdminMetricasRepository} (y las arma en un único
 * {@link MetricasAdminDTO} para que el frontend resuelva el dashboard con una sola llamada) y la
 * moderación en {@link AdminModeracionRepository}.
 */
@Service
public class AdminService {

    /** Zona horaria del negocio: "este mes" se calcula según el calendario de Argentina. */
    static final ZoneId ZONA_NEGOCIO = ZoneId.of("America/Argentina/Buenos_Aires");

    /** Tamaño de cada ranking del panel. */
    private static final Pageable TOP_5 = PageRequest.of(0, 5);

    /** Límites de filas para la tabla de moderación, para no traer toda la tabla de reseñas. */
    static final int LIMITE_MINIMO = 1;
    static final int LIMITE_MAXIMO = 50;

    private final AdminMetricasRepository metricasRepo;
    private final AdminModeracionRepository moderacionRepo;
    private final Clock clock;

    @Autowired
    public AdminService(AdminMetricasRepository metricasRepo, AdminModeracionRepository moderacionRepo) {
        this(metricasRepo, moderacionRepo, Clock.system(ZONA_NEGOCIO));
    }

    /** Permite fijar la fecha actual en los tests. */
    AdminService(AdminMetricasRepository metricasRepo, AdminModeracionRepository moderacionRepo, Clock clock) {
        this.metricasRepo = metricasRepo;
        this.moderacionRepo = moderacionRepo;
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

    /**
     * Reseñas más recientes de la plataforma para la tabla de moderación.
     * <p>
     * El límite se acota entre {@value #LIMITE_MINIMO} y {@value #LIMITE_MAXIMO}: un valor fuera de
     * rango no da error, se ajusta al extremo más cercano.
     *
     * @param limite        cantidad de reseñas pedida por el panel
     * @param pagina        página, desde 0
     * @param texto         texto a buscar en el comentario, el autor o el restaurante, sin distinguir
     *                      mayúsculas; vacío o {@code null} no filtra
     * @param calificacion  calificación exacta (1 a 5); {@code null} no filtra
     * @param restauranteId solo reseñas de ese restaurante; {@code null} no filtra
     * @return reseñas de la más nueva a la más vieja
     * @throws BusinessException si la calificación no está entre 1 y 5 o la página es negativa
     */
    @Transactional(readOnly = true)
    public List<ResenaModeracionDTO> resenasRecientes(int limite, int pagina, String texto,
                                                      Integer calificacion, Long restauranteId) {
        if (calificacion != null && (calificacion < 1 || calificacion > 5)) {
            throw new BusinessException("La calificación debe estar entre 1 y 5");
        }
        if (pagina < 0) {
            throw new BusinessException("La página no puede ser negativa");
        }
        int acotado = Math.clamp(limite, LIMITE_MINIMO, LIMITE_MAXIMO);
        String patron = texto == null || texto.isBlank() ? null : "%" + texto.strip().toLowerCase() + "%";
        return moderacionRepo.resenasRecientes(patron, calificacion, restauranteId, PageRequest.of(pagina, acotado));
    }
}
