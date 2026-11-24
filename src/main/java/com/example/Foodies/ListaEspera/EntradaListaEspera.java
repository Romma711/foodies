package com.example.Foodies.ListaEspera;

import com.example.Foodies.Restaurant.Restaurant;
import com.example.Foodies.Turno.Turno;
import com.example.Foodies.Usuario.Usuario;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * A client waiting for a spot at a restaurant on a date and time or shift (RF25, D15). One open
 * entry per client, restaurant and date.
 */
@Entity
@Table(name = "lista_espera")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EntradaListaEspera {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Usuario usuario;

    @ManyToOne(optional = false)
    @JoinColumn(name = "restaurant_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Restaurant restaurant;

    @Column(nullable = false)
    private LocalDate fecha;

    /** Shift when the restaurant works with shifts; null for per-day capacity. */
    @ManyToOne
    @JoinColumn(name = "turno_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Turno turno;

    /** Arrival time wanted (the shift start with shifts). */
    @Column(nullable = false)
    private LocalTime horario;

    @Column(nullable = false)
    private Integer cantidad;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private EstadoListaEspera estado = EstadoListaEspera.ESPERANDO;

    @Column(nullable = false)
    private LocalDateTime creada;

    private LocalDateTime notificada;

    public boolean abierta() {
        return estado == EstadoListaEspera.ESPERANDO || estado == EstadoListaEspera.NOTIFICADA;
    }
}
