package com.example.Foodies.CupoEspecial;

import com.example.Foodies.Espacio.Espacio;
import com.example.Foodies.Restaurant.Restaurant;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDate;

/**
 * Capacidad reducida de un espacio para un día puntual (por ejemplo, el patio cerrado por lluvia).
 * Ese día reemplaza a la capacidad del espacio; los demás días y los demás espacios no cambian.
 * {@code restaurant} se mantiene para listar los cupos del restaurante sin recorrer sus espacios.
 */
@Entity
@Table(name = "CuposEspeciales",
        uniqueConstraints = @UniqueConstraint(columnNames = {"espacio_id", "fecha"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CupoEspecial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "restaurant_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Restaurant restaurant;

    /** Espacio al que aplica. Nullable en la base solo por las filas previas al backfill. */
    @ManyToOne
    @JoinColumn(name = "espacio_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Espacio espacio;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false)
    private Integer cupo;

    @Column(length = 120)
    private String motivo;
}
