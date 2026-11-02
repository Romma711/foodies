package com.example.Foodies.Moderacion.dtos;

/**
 * Resultado de registrar un strike sobre un usuario o un restaurante.
 *
 * @param id           id del usuario o restaurante sancionado
 * @param nombre       nombre visible del sancionado
 * @param email        casilla a la que se envió el aviso
 * @param strikes      strikes acumulados después de este
 * @param sugerirBaja  {@code true} al llegar al límite de strikes: el panel sugiere dar de baja
 * @param emailEnviado {@code false} si el aviso no se pudo enviar; el strike igual queda registrado
 */
public record StrikeResultadoDTO(
        Long id,
        String nombre,
        String email,
        Integer strikes,
        boolean sugerirBaja,
        boolean emailEnviado
) {
}
