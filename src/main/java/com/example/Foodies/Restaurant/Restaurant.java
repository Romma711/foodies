package com.example.Foodies.Restaurant;

import com.example.Foodies.CupoEspecial.CupoEspecial;
import com.example.Foodies.Foto.FotoRestaurante;
import com.example.Foodies.Carta.Carta;
import com.example.Foodies.Enums.EspecialidadDeComida;
import com.example.Foodies.Enums.EstadoRestaurante;
import com.example.Foodies.Resena.Resena;
import com.example.Foodies.Reserva.Reserva;
import com.example.Foodies.Usuario.Usuario;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

import java.util.ArrayList;
import java.util.List;

@Table(name = "Restaurants")
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Restaurant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @NotBlank(message = "Nombre de restaurant es obliglatorio")
    private String nombre;

    @Column(nullable = false)
    @Min(value = 1,message = "El cupo debe ser mayor a 1")
    private Integer cupoMaximo;

    /** Porcentaje del cupo diario que se acepta solo (0-100). Null o 0 = todas las reservas quedan pendientes. */
    private Integer porcentajeAutoAceptacion;


    @Column(nullable = false)
    @NotBlank(message = "La ubicacion es obligatoria")
    private String ubicacion;

    @NotNull
    private boolean aprobado = false;

    /** Latitud para el mapa. Queda en {@code null} hasta que un administrador carga la ubicación. */
    private Double latitud;

    /** Longitud para el mapa. Queda en {@code null} hasta que un administrador carga la ubicación. */
    private Double longitud;

    /**
     * Estado de la solicitud de alta. Se mantiene sincronizado con {@link #aprobado}
     * (ver {@link #sincronizarEstado()}). Las filas anteriores a esta columna se completan al
     * arrancar con {@code EstadoRestauranteBackfill}.
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private EstadoRestaurante estado;

    /** Sanciones acumuladas. La columna tiene default 0 para no dejar en null las filas existentes. */
    @ColumnDefault("0")
    @Column(nullable = false)
    private Integer strikes = 0;

    /** Baja lógica: {@code false} oculta el restaurante sin borrar sus reservas ni reseñas. */
    @ColumnDefault("true")
    @Column(nullable = false)
    private Boolean activo = true;



    @OneToOne(mappedBy = "restaurant", cascade = CascadeType.ALL, orphanRemoval = true)
    private Carta carta;

     @Enumerated(EnumType.STRING)
     private EspecialidadDeComida especialidad;

     @OneToOne(orphanRemoval = true, cascade = CascadeType.ALL)
     @JoinColumn(name = "usuario_id",nullable = false)
     private Usuario usuario;

     @OneToMany(mappedBy = "restaurant", orphanRemoval = true, cascade = CascadeType.ALL)
     private List<Reserva> reserva = new ArrayList<>();

     @OneToMany(mappedBy = "restaurant", orphanRemoval = true, cascade = CascadeType.ALL)
     private List<Resena> resenas = new ArrayList<>();

     @OneToMany(mappedBy = "restaurant", orphanRemoval = true, cascade = CascadeType.ALL)
     private List<CupoEspecial> cuposEspeciales = new ArrayList<>();

     @OneToMany(mappedBy = "restaurant", orphanRemoval = true, cascade = CascadeType.ALL)
     private List<FotoRestaurante> fotos = new ArrayList<>();

    /**
     * Mantiene {@link #estado} coherente con {@link #aprobado} antes de cada insert/update.
     *
     * <ul>
     *   <li>Sin estado: se deriva de {@code aprobado} (true → APROBADO, false → PENDIENTE).</li>
     *   <li>{@code aprobado = true}: el estado pasa a APROBADO. Así el endpoint de aprobación
     *       existente ({@code PUT /api/admin/approved/{usuarioId}}) también actualiza el estado.</li>
     *   <li>{@code aprobado = false} con estado APROBADO: vuelve a PENDIENTE.</li>
     * </ul>
     * Un RECHAZADO con {@code aprobado = false} se respeta.
     */
    @PrePersist
    @PreUpdate
    void sincronizarEstado() {
        if (aprobado) {
            estado = EstadoRestaurante.APROBADO;
        } else if (estado == null || estado == EstadoRestaurante.APROBADO) {
            estado = EstadoRestaurante.PENDIENTE;
        }
    }

}