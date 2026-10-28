package com.example.Foodies.Moderacion;

/**
 * Texto del email de aviso de sanción. Está separado del envío para poder cambiar el contenido
 * sin tocar la configuración de correo.
 */
final class PlantillaAvisoSancion {

    private PlantillaAvisoSancion() {
    }

    /**
     * @param strikes strikes acumulados
     * @param limite  límite de strikes
     * @return asunto, por ejemplo "Foodies: aviso de sanción (2 de 3)"
     */
    static String asunto(int strikes, int limite) {
        return "Foodies: aviso de sanción (%d de %d)".formatted(strikes, limite);
    }

    /**
     * @param nombre  nombre con el que se saluda; si viene vacío se usa un saludo genérico
     * @param strikes strikes acumulados
     * @param limite  límite de strikes
     * @param motivo  motivo de la sanción
     * @return cuerpo en texto plano
     */
    static String cuerpo(String nombre, int strikes, int limite, String motivo) {
        String saludo = nombre == null || nombre.isBlank() ? "Hola" : "Hola, " + nombre;
        String cierre = strikes >= limite
                ? "Alcanzaste el límite de sanciones. Tu cuenta puede ser dada de baja."
                : "Al llegar a %d sanciones la cuenta puede ser dada de baja.".formatted(limite);
        return """
                %s:

                Recibiste una sanción en Foodies.

                Motivo: %s
                Sanciones acumuladas: %d de %d

                %s

                Si creés que se trata de un error, respondé este correo.

                El equipo de Foodies
                """.formatted(saludo, motivo, strikes, limite, cierre);
    }
}
