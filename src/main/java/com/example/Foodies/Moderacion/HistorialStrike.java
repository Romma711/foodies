package com.example.Foodies.Moderacion;

import com.example.Foodies.Restaurant.Restaurant;
import com.example.Foodies.Usuario.Usuario;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * One change of the strike counter of a user or a restaurant (S7d): who, when, added or removed, by
 * the admin or automatically for no-shows, why, and the total it left. Exactly one of
 * {@code usuario} and {@code restaurant} is set. Strikes given before this table existed have no row.
 * Physically deleting the account or restaurant (admin only, when it has no history of its own)
 * deletes its rows too (ON DELETE CASCADE).
 */
@Entity
@Table(name = "historial_strikes")
@Data
@NoArgsConstructor
public class HistorialStrike {

    /** Whether the change added or removed a strike. */
    public enum Tipo { SUMA, QUITA }

    /** Who caused the change: the admin, or the automatic strike for no-shows (D5). */
    public enum Origen { ADMIN, AUSENCIAS }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "usuario_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Usuario usuario;

    @ManyToOne
    @JoinColumn(name = "restaurant_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Restaurant restaurant;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 10)
    private Tipo tipo;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 10)
    private Origen origen;

    @Column(nullable = false, length = 255)
    private String motivo;

    @Column(nullable = false)
    private Integer totalDespues;
}
