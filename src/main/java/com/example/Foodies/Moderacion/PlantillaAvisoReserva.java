package com.example.Foodies.Moderacion;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Texts of the reservation emails other than the confirmation (D10): new reservation for the
 * restaurant, rejection and cancellations, and the waitlist notice.
 */
final class PlantillaAvisoReserva {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final String FIRMA = "\nEl equipo de Foodies\n";

    private PlantillaAvisoReserva() {
    }

    /** Data of one reservation copied while the session is open. */
    record Datos(String cliente, String restaurante, LocalDate fecha, LocalTime hora, int personas) {

        String detalle() {
            return "Fecha: %s%nHora: %s%nPersonas: %d%n".formatted(FECHA.format(fecha), HORA.format(hora), personas);
        }
    }

    static String asuntoNueva(Datos d) {
        return "Foodies: nueva reserva en %s".formatted(d.restaurante());
    }

    static String cuerpoNueva(Datos d, boolean aceptada) {
        return """
                Hola:

                %s hizo una reserva en %s.

                %sEstado: %s

                Podés verla en tu panel.
                %s""".formatted(d.cliente(), d.restaurante(), d.detalle(),
                aceptada ? "aceptada automáticamente" : "pendiente de confirmación", FIRMA);
    }

    static String asuntoRechazada(Datos d) {
        return "Foodies: tu reserva en %s no fue confirmada".formatted(d.restaurante());
    }

    static String cuerpoRechazada(Datos d) {
        return """
                %s:

                %s no pudo confirmar tu reserva.

                %s
                Probá con otro horario o con otro restaurante.
                %s""".formatted(saludo(d.cliente()), d.restaurante(), d.detalle(), FIRMA);
    }

    static String asuntoCanceladaPorRestaurante(Datos d) {
        return "Foodies: %s canceló tu reserva".formatted(d.restaurante());
    }

    static String cuerpoCanceladaPorRestaurante(Datos d) {
        return """
                %s:

                %s canceló tu reserva.

                %s
                Si tenés dudas, comunicate con el restaurante.
                %s""".formatted(saludo(d.cliente()), d.restaurante(), d.detalle(), FIRMA);
    }

    static String asuntoCanceladaParaRestaurante(Datos d) {
        return "Foodies: se canceló una reserva en %s".formatted(d.restaurante());
    }

    static String cuerpoCanceladaPorCliente(Datos d) {
        return """
                Hola:

                %s canceló su reserva en %s. El lugar ya quedó libre.

                %s%s""".formatted(d.cliente(), d.restaurante(), d.detalle(), FIRMA);
    }

    static String cuerpoCanceladaPorBajaDelCliente(Datos d) {
        return """
                Hola:

                La cuenta de %s fue dada de baja y su reserva en %s quedó cancelada. El lugar ya quedó libre.

                %s%s""".formatted(d.cliente(), d.restaurante(), d.detalle(), FIRMA);
    }

    static String asuntoCanceladaPorBajaDelRestaurante(Datos d) {
        return "Foodies: tu reserva en %s fue cancelada".formatted(d.restaurante());
    }

    static String cuerpoCanceladaPorBajaDelRestaurante(Datos d) {
        return """
                %s:

                %s dejó de recibir reservas en Foodies y tu reserva quedó cancelada.

                %s
                Perdón por las molestias. Podés buscar otro restaurante en Foodies.
                %s""".formatted(saludo(d.cliente()), d.restaurante(), d.detalle(), FIRMA);
    }

    static String asuntoLugarLibre(String restaurante) {
        return "Foodies: se liberó un lugar en %s".formatted(restaurante);
    }

    static String cuerpoLugarLibre(String cliente, String restaurante, LocalDate fecha, LocalTime hora, int personas) {
        return """
                %s:

                Se liberó un lugar en %s para el %s a las %s (%d %s), la fecha en la que te anotaste en la lista de espera.

                El lugar no queda guardado: reservá desde Foodies cuanto antes, el primero que reserva se lo queda.
                %s""".formatted(saludo(cliente), restaurante, FECHA.format(fecha), HORA.format(hora), personas,
                personas == 1 ? "persona" : "personas", FIRMA);
    }

    private static String saludo(String nombre) {
        return nombre == null || nombre.isBlank() ? "Hola" : "Hola, " + nombre;
    }
}
