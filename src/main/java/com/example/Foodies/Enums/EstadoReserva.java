package com.example.Foodies.Enums;

import java.util.EnumSet;
import java.util.Set;

/**
 * Lifecycle of a reservation. Stored as text, so new values never shift the existing rows.
 *
 * <ul>
 *   <li>{@code PENDIENTE}: waiting for the restaurant.</li>
 *   <li>{@code ACEPTADA}: confirmed by the restaurant or by auto-acceptance.</li>
 *   <li>{@code CANCELADA}: cancelled by the client, by the restaurant or by an account deactivation.</li>
 *   <li>{@code RECHAZADA}: the restaurant rejected a pending reservation.</li>
 *   <li>{@code ASISTIO} / {@code NO_ASISTIO}: attendance marked by the restaurant after the arrival time.</li>
 *   <li>{@code VENCIDA}: still pending when its arrival time passed (closed by a scheduled job).</li>
 * </ul>
 * Every state except {@code PENDIENTE} and {@code ACEPTADA} is final.
 */
public enum EstadoReserva {
    ACEPTADA,
    PENDIENTE,
    CANCELADA,
    RECHAZADA,
    ASISTIO,
    NO_ASISTIO,
    VENCIDA;

    /** The only states that hold capacity and count as a client's active reservation. */
    public static final Set<EstadoReserva> ACTIVOS = EnumSet.of(PENDIENTE, ACEPTADA);

    /** True for {@code PENDIENTE} and {@code ACEPTADA}: the reservation holds capacity. */
    public boolean esActiva() {
        return ACTIVOS.contains(this);
    }

    /** True when the reservation can no longer change. */
    public boolean esFinal() {
        return !esActiva();
    }
}
