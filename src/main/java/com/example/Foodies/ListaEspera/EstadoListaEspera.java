package com.example.Foodies.ListaEspera;

/** State of a waitlist entry (RF25). Only ESPERANDO and NOTIFICADA are open. */
public enum EstadoListaEspera {
    /** Waiting for a spot. */
    ESPERANDO,
    /** A spot freed up and the client was emailed; the spot is not held. */
    NOTIFICADA,
    /** The date and time passed. */
    VENCIDA,
    /** The client left the list, or booked that restaurant and day. */
    CANCELADA
}
