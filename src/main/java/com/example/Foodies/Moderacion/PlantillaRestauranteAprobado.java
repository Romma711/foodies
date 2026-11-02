package com.example.Foodies.Moderacion;

/**
 * Texto del email que recibe el encargado cuando el administrador aprueba su restaurante.
 */
final class PlantillaRestauranteAprobado {

    private PlantillaRestauranteAprobado() {
    }

    /**
     * @param restaurante nombre del restaurante
     * @return asunto, por ejemplo "Foodies: tu restaurante Pasta Nostra fue aprobado"
     */
    static String asunto(String restaurante) {
        return "Foodies: tu restaurante %s fue aprobado".formatted(restaurante);
    }

    /**
     * @param restaurante nombre del restaurante
     * @return cuerpo en texto plano
     */
    static String cuerpo(String restaurante) {
        return """
                Hola:

                ¡Buenas noticias! Revisamos la solicitud de %s y la aprobamos.

                Tu restaurante ya aparece en Foodies y los clientes pueden empezar a reservar.

                Desde tu panel podés:
                - Aceptar o cancelar las reservas que lleguen.
                - Actualizar los datos del restaurante y el cupo máximo.
                - Subir la carta.

                Ingresá con el email y la contraseña con los que te registraste.

                ¡Bienvenido a Foodies!
                El equipo de Foodies
                """.formatted(restaurante);
    }
}
