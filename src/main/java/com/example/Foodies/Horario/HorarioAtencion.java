package com.example.Foodies.Horario;

import com.example.Foodies.Restaurant.Restaurant;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * Franja en la que un restaurante recibe reservas un día de la semana (por ejemplo, sábado de 12:00
 * a 16:00). Un día puede tener varias franjas, como almuerzo y cena. Un {@code cierre} a las 00:00
 * significa medianoche: la franja llega hasta el final del día.
 */
@Entity
@Table(name = "HorariosAtencion")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HorarioAtencion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "restaurant_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Restaurant restaurant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private DayOfWeek dia;

    @Column(nullable = false)
    private LocalTime apertura;

    @Column(nullable = false)
    private LocalTime cierre;

    /** True si {@code hora} cae en la franja: desde la apertura y antes del cierre. */
    public boolean incluye(LocalTime hora) {
        return !hora.isBefore(apertura) && (cierre.equals(LocalTime.MIDNIGHT) || hora.isBefore(cierre));
    }
}
