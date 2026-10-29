package com.example.Foodies.Moderacion;

import com.example.Foodies.Enums.EstadoReserva;
import com.example.Foodies.Reserva.Reserva;
import com.example.Foodies.Restaurant.Restaurant;
import com.example.Foodies.Usuario.Usuario;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.PostUpdateEvent;
import org.hibernate.event.spi.PostUpdateEventListener;
import org.hibernate.persister.entity.EntityPersister;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;

/**
 * Le envía al cliente el email de reserva confirmada cuando una reserva pasa a {@code ACEPTADA},
 * sin modificar el servicio ni el controlador de reservas: escucha la actualización de la entidad
 * en Hibernate.
 *
 * <p>Los datos se copian durante el flush, con la sesión abierta, y el email sale recién después
 * del commit (ver {@link DespuesDelCommit}). Volver a guardar una reserva que ya estaba aceptada no
 * repite el aviso.</p>
 */
@Component
public class ReservaConfirmadaListener implements PostUpdateEventListener {

    private static final String ESTADO = "estadoReserva";

    private final EntityManagerFactory entityManagerFactory;
    private final EmailService emailService;

    public ReservaConfirmadaListener(EntityManagerFactory entityManagerFactory, EmailService emailService) {
        this.entityManagerFactory = entityManagerFactory;
        this.emailService = emailService;
    }

    @PostConstruct
    void registrar() {
        entityManagerFactory.unwrap(SessionFactoryImplementor.class)
                .getServiceRegistry()
                .requireService(EventListenerRegistry.class)
                .appendListeners(EventType.POST_UPDATE, this);
    }

    @Override
    public void onPostUpdate(PostUpdateEvent event) {
        if (!(event.getEntity() instanceof Reserva reserva) || !recienAceptada(event, reserva)) {
            return;
        }
        Usuario cliente = reserva.getUsuario();
        Restaurant restaurant = reserva.getRestaurant();
        if (cliente == null || cliente.getEmail() == null || restaurant == null
                || reserva.getFechaReserva() == null || reserva.getHorariollegada() == null) {
            return;
        }
        ReservaConfirmada datos = new ReservaConfirmada(
                cliente.getEmail(),
                cliente.getNombre(),
                restaurant.getNombre(),
                restaurant.getUbicacion(),
                aLocalDate(reserva.getFechaReserva()),
                reserva.getHorariollegada(),
                reserva.getCantidad() == null ? 0 : reserva.getCantidad());
        DespuesDelCommit.ejecutar(() -> emailService.enviarReservaConfirmada(datos));
    }

    @Override
    public boolean requiresPostCommitHandling(EntityPersister persister) {
        return false;
    }

    private static boolean recienAceptada(PostUpdateEvent event, Reserva reserva) {
        if (reserva.getEstadoReserva() != EstadoReserva.ACEPTADA) {
            return false;
        }
        Object[] anterior = event.getOldState();
        int indice = Arrays.asList(event.getPersister().getPropertyNames()).indexOf(ESTADO);
        // Without the previous state (detached update) the change cannot be proven, so no email.
        return anterior != null && indice >= 0 && anterior[indice] != EstadoReserva.ACEPTADA;
    }

    /** {@code fechaReserva} may come back from JPA as {@code java.sql.Date}/{@code Timestamp}. */
    private static LocalDate aLocalDate(Date fecha) {
        if (fecha instanceof java.sql.Date sql) {
            return sql.toLocalDate();
        }
        if (fecha instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime().toLocalDate();
        }
        return fecha.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
}
