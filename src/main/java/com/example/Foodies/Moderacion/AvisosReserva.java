package com.example.Foodies.Moderacion;

import com.example.Foodies.Enums.EstadoReserva;
import com.example.Foodies.Reserva.Reserva;
import com.example.Foodies.Restaurant.Restaurant;
import com.example.Foodies.Usuario.Usuario;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Reservation emails other than the confirmation (D10). The data is copied right away, while the
 * session is open, and the email goes out after the commit (see {@link DespuesDelCommit}); a mail
 * failure is logged and never rolls back the operation. The confirmation stays in
 * {@link ReservaConfirmadaListener}.
 */
@Component
public class AvisosReserva {

    private final EmailService emailService;

    public AvisosReserva(EmailService emailService) {
        this.emailService = emailService;
    }

    /** To the restaurant: a client booked (pending or auto-accepted). */
    public void nuevaReserva(Reserva reserva) {
        boolean aceptada = reserva.getEstadoReserva() == EstadoReserva.ACEPTADA;
        alRestaurante(reserva,
                (email, d) -> enviar(email, "el aviso de reserva nueva",
                        PlantillaAvisoReserva.asuntoNueva(d), PlantillaAvisoReserva.cuerpoNueva(d, aceptada)));
    }

    /** To the client: the restaurant rejected the pending reservation. */
    public void rechazada(Reserva reserva) {
        alCliente(reserva, (email, d) -> enviar(email, "el aviso de reserva rechazada",
                PlantillaAvisoReserva.asuntoRechazada(d), PlantillaAvisoReserva.cuerpoRechazada(d)));
    }

    /** To the client: the restaurant cancelled the reservation. */
    public void canceladaPorRestaurante(Reserva reserva) {
        alCliente(reserva, (email, d) -> enviar(email, "el aviso de cancelación",
                PlantillaAvisoReserva.asuntoCanceladaPorRestaurante(d),
                PlantillaAvisoReserva.cuerpoCanceladaPorRestaurante(d)));
    }

    /** To the restaurant: the client cancelled. */
    public void canceladaPorCliente(Reserva reserva) {
        alRestaurante(reserva,
                (email, d) -> enviar(email, "el aviso de cancelación",
                        PlantillaAvisoReserva.asuntoCanceladaParaRestaurante(d),
                        PlantillaAvisoReserva.cuerpoCanceladaPorCliente(d)));
    }

    /** To the restaurant: the client's account was deactivated and the reservation cancelled. */
    public void canceladaPorBajaDelCliente(Reserva reserva) {
        alRestaurante(reserva,
                (email, d) -> enviar(email, "el aviso de cancelación",
                        PlantillaAvisoReserva.asuntoCanceladaParaRestaurante(d),
                        PlantillaAvisoReserva.cuerpoCanceladaPorBajaDelCliente(d)));
    }

    /** To the client: the restaurant (or its manager) was deactivated and the reservation cancelled. */
    public void canceladaPorBajaDelRestaurante(Reserva reserva) {
        alCliente(reserva, (email, d) -> enviar(email, "el aviso de cancelación",
                PlantillaAvisoReserva.asuntoCanceladaPorBajaDelRestaurante(d),
                PlantillaAvisoReserva.cuerpoCanceladaPorBajaDelRestaurante(d)));
    }

    /** To a waitlisted client: a spot freed up; it is not held. */
    public void lugarLibre(Usuario cliente, Restaurant restaurant, LocalDate fecha, LocalTime hora, int personas) {
        if (cliente == null || cliente.getEmail() == null || restaurant == null) {
            return;
        }
        String email = cliente.getEmail();
        String nombre = cliente.getNombre();
        String restaurante = restaurant.getNombre();
        enviar(email, "el aviso de lista de espera", PlantillaAvisoReserva.asuntoLugarLibre(restaurante),
                PlantillaAvisoReserva.cuerpoLugarLibre(nombre, restaurante, fecha, hora, personas));
    }

    private void alCliente(Reserva reserva, BiConsumer<String, PlantillaAvisoReserva.Datos> envio) {
        PlantillaAvisoReserva.Datos datos = datos(reserva);
        Usuario cliente = reserva.getUsuario();
        if (datos != null && cliente != null && cliente.getEmail() != null) {
            envio.accept(cliente.getEmail(), datos);
        }
    }

    private void alRestaurante(Reserva reserva, BiConsumer<String, PlantillaAvisoReserva.Datos> envio) {
        PlantillaAvisoReserva.Datos datos = datos(reserva);
        Restaurant restaurant = reserva.getRestaurant();
        Usuario encargado = restaurant == null ? null : restaurant.getUsuario();
        if (datos != null && encargado != null && encargado.getEmail() != null) {
            envio.accept(encargado.getEmail(), datos);
        }
    }

    private void enviar(String email, String descripcion, String asunto, String cuerpo) {
        DespuesDelCommit.ejecutar(() -> emailService.enviarAviso(email, descripcion, asunto, cuerpo));
    }

    private static PlantillaAvisoReserva.Datos datos(Reserva reserva) {
        if (reserva.getRestaurant() == null || reserva.fechaLocal() == null || reserva.getHorariollegada() == null) {
            return null;
        }
        Usuario cliente = reserva.getUsuario();
        String nombre = cliente == null ? "Un cliente" : Stream.of(cliente.getNombre(), cliente.getApellido())
                .filter(parte -> parte != null && !parte.isBlank())
                .collect(Collectors.joining(" "));
        return new PlantillaAvisoReserva.Datos(nombre.isBlank() ? "Un cliente" : nombre,
                reserva.getRestaurant().getNombre(), reserva.fechaLocal(), reserva.getHorariollegada(),
                reserva.getCantidad() == null ? 0 : reserva.getCantidad());
    }
}
