package com.example.Foodies.CupoEspecial;

import com.example.Foodies.Restaurant.Restaurant;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDate;

/**
 * Capacidad reducida de un restaurante para un día puntual (por ejemplo, un evento privado).
 * Ese día reemplaza a {@link Restaurant#getCupoMaximo()}; los demás días no cambian.
 */
@Entity
@Table(name = "CuposEspeciales",
        uniqueConstraints = @UniqueConstraint(columnNames = {"restaurant_id", "fecha"}))
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

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false)
    private Integer cupo;

    @Column(length = 120)
    private String motivo;
}
