package com.example.Foodies.Moderacion.dtos;

/**
 * Body to remove a strike (S7c). The reason is checked by the service so a blank one answers with a
 * {@code codigo}.
 *
 * @param motivo why the strike is removed; kept in the strike history
 */
public record QuitarStrikeRequestDTO(String motivo) {
}
