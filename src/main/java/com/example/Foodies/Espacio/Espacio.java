package com.example.Foodies.Espacio;

import com.example.Foodies.Restaurant.Restaurant;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.ColumnDefault;

/**
 * Sector de un restaurante (salón, patio o barra) con capacidad propia por día.
 * El cliente elige un espacio al reservar y el cupo se valida contra ese espacio.
 * Un espacio no se borra: se desactiva para no perder sus reservas.
 */
@Entity
@Table(name = "Espacios",
        uniqueConstraints = @UniqueConstraint(columnNames = {"restaurant_id", "nombre"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Espacio {

    public static final int MAX_NOMBRE = 60;
    public static final String NOMBRE_POR_DEFECTO = "Salón principal";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "restaurant_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Restaurant restaurant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoEspacio tipo;

    @Column(nullable = false, length = MAX_NOMBRE)
    private String nombre;

    @Column(nullable = false)
    private Integer capacidad;

    @ColumnDefault("true")
    @Column(nullable = false)
    private Boolean activo = true;

    public static Espacio salonPrincipal(Restaurant restaurant, int capacidad) {
        Espacio espacio = new Espacio();
        espacio.setRestaurant(restaurant);
        espacio.setTipo(TipoEspacio.SALON);
        espacio.setNombre(NOMBRE_POR_DEFECTO);
        espacio.setCapacidad(capacidad);
        espacio.setActivo(true);
        return espacio;
    }
}
