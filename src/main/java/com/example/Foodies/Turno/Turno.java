package com.example.Foodies.Turno;

import com.example.Foodies.Restaurant.Restaurant;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * Seating of a restaurant on a day of the week (RF24, D2), e.g. Saturday 20:00 for 90 minutes.
 * Each space's capacity applies per shift and the arrival time is the shift start. A restaurant
 * without shifts keeps the per-day capacity. Shifts are never deleted (they keep their
 * reservations): removing one sets {@code activo = false}.
 */
@Entity
@Table(name = "Turnos")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Turno {

    public static final int DURACION_POR_DEFECTO = 90;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "restaurant_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Restaurant restaurant;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 10)
    private DayOfWeek dia;

    @Column(nullable = false)
    private LocalTime inicio;

    @Column(nullable = false)
    private Integer duracionMinutos = DURACION_POR_DEFECTO;

    @ColumnDefault("true")
    @Column(nullable = false)
    private Boolean activo = true;

    /** Minutes from midnight when the shift ends (may be 24:00). */
    public int finEnMinutos() {
        return inicio.getHour() * 60 + inicio.getMinute() + duracionMinutos;
    }

    /** End time; a shift ending at midnight shows 00:00. */
    public LocalTime fin() {
        int fin = finEnMinutos() % (24 * 60);
        return LocalTime.of(fin / 60, fin % 60);
    }

    /** True if {@code hora} falls in [inicio, fin). */
    public boolean incluye(LocalTime hora) {
        int minutos = hora.getHour() * 60 + hora.getMinute();
        int desde = inicio.getHour() * 60 + inicio.getMinute();
        return minutos >= desde && minutos < finEnMinutos();
    }
}
