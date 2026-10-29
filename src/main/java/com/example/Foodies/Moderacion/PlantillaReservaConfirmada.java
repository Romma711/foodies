package com.example.Foodies.Moderacion;

import java.time.format.DateTimeFormatter;

/**
 * Texto del email que recibe el cliente cuando el restaurante acepta su reserva.
 */
final class PlantillaReservaConfirmada {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private PlantillaReservaConfirmada() {
    }

    /**
     * @param restaurante nombre del restaurante
     * @return asunto, por ejemplo "Foodies: tu reserva en Pasta Nostra está confirmada"
     */
    static String asunto(String restaurante) {
        return "Foodies: tu reserva en %s está confirmada".formatted(restaurante);
    }

    /**
     * @param reserva datos de la reserva confirmada
     * @return cuerpo en texto plano
     */
    static String cuerpo(ReservaConfirmada reserva) {
        String saludo = reserva.nombreCliente() == null || reserva.nombreCliente().isBlank()
                ? "Hola"
                : "Hola, " + reserva.nombreCliente();
        return """
                %s:

                ¡Tu reserva está confirmada! %s te espera.

                Fecha: %s
                Hora: %s
                Personas: %d
                Dirección: %s

                Si no podés ir, avisale al restaurante así el lugar queda libre para otra persona.

                ¡Que lo disfrutes!
                El equipo de Foodies
                """.formatted(saludo, reserva.restaurante(), FECHA.format(reserva.fecha()), HORA.format(reserva.hora()),
                reserva.personas(), reserva.direccion() == null ? "-" : reserva.direccion());
    }
}
