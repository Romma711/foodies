package com.example.Foodies.ADMIN;

import com.example.Foodies.ADMIN.dtos.MetricasAdminDTO;
import com.example.Foodies.ADMIN.dtos.ResenaModeracionDTO;
import com.example.Foodies.ADMIN.dtos.RestauranteCalificacionDTO;
import com.example.Foodies.ADMIN.dtos.RestauranteReservasDTO;
import com.example.Foodies.ADMIN.dtos.UsuarioResenasDTO;
import com.example.Foodies.Enums.EstadoReserva;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminServiceTest {

    private static final ZoneId ZONA = ZoneId.of("America/Argentina/Buenos_Aires");
    private static final PageRequest TOP_5 = PageRequest.of(0, 5);

    private final AdminMetricasRepository repo = mock(AdminMetricasRepository.class);
    private final AdminModeracionRepository moderacionRepo = mock(AdminModeracionRepository.class);

    private AdminService servicioEn(int anio, int mes, int dia) {
        Clock clock = Clock.fixed(ZonedDateTime.of(anio, mes, dia, 10, 0, 0, 0, ZONA).toInstant(), ZONA);
        return new AdminService(repo, moderacionRepo, clock);
    }

    @Test
    void obtenerMetricasArmaElDtoConLosTopsYElMesEnCurso() {
        List<RestauranteCalificacionDTO> calificados = List.of(new RestauranteCalificacionDTO(5L, "Ochiwan", 4.5, 2L));
        List<RestauranteReservasDTO> reservados = List.of(new RestauranteReservasDTO(5L, "Ochiwan", 7L));
        List<UsuarioResenasDTO> resenadores = List.of(new UsuarioResenasDTO(3L, "Ana", "Gómez", 4L));
        when(repo.topRestaurantesMejorCalificados(TOP_5)).thenReturn(calificados);
        when(repo.topRestaurantesConMasReservas(EstadoReserva.CANCELADA, TOP_5)).thenReturn(reservados);
        when(repo.topUsuariosConMasResenas(TOP_5)).thenReturn(resenadores);
        when(repo.contarUsuariosRegistradosEntre(
                LocalDateTime.of(2026, 10, 1, 0, 0),
                LocalDateTime.of(2026, 11, 1, 0, 0))).thenReturn(12L);

        MetricasAdminDTO metricas = servicioEn(2026, 10, 26).obtenerMetricas();

        assertThat(metricas.topRestaurantesMejorCalificados()).isEqualTo(calificados);
        assertThat(metricas.topRestaurantesConMasReservas()).isEqualTo(reservados);
        assertThat(metricas.topUsuariosConMasResenas()).isEqualTo(resenadores);
        assertThat(metricas.usuariosRegistradosEsteMes()).isEqualTo(12L);
        assertThat(metricas.periodo()).isEqualTo("2026-10");
    }

    @Test
    void elRangoDelMesCruzaDeAnioEnDiciembre() {
        servicioEn(2026, 12, 31).obtenerMetricas();

        verify(repo).contarUsuariosRegistradosEntre(
                LocalDateTime.of(2026, 12, 1, 0, 0),
                LocalDateTime.of(2027, 1, 1, 0, 0));
    }

    @Test
    void resenasRecientesUsaElLimitePedido() {
        List<ResenaModeracionDTO> recientes =
                List.of(new ResenaModeracionDTO(9L, "Muy rico", 5, "Ana Gómez", 5L, "Ochiwan"));
        when(moderacionRepo.resenasRecientes(PageRequest.of(0, 10))).thenReturn(recientes);

        assertThat(servicioEn(2026, 10, 26).resenasRecientes(10)).isEqualTo(recientes);
    }

    @Test
    void resenasRecientesAcotaElLimiteEntreUnoYCincuenta() {
        AdminService servicio = servicioEn(2026, 10, 26);

        servicio.resenasRecientes(0);
        servicio.resenasRecientes(500);

        verify(moderacionRepo).resenasRecientes(PageRequest.of(0, 1));
        verify(moderacionRepo).resenasRecientes(PageRequest.of(0, 50));
    }
}
