package com.example.Foodies.Reserva;

import com.example.Foodies.Enums.EstadoReserva;
import com.example.Foodies.Espacio.Espacio;
import com.example.Foodies.Restaurant.Restaurant;
import com.example.Foodies.Usuario.Usuario;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.Date;

@Entity
@Table(name = "reserva")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Reserva {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @NotNull
    private Integer cantidad;

    @NotNull
    private Date fechaReserva;

    @NotNull
    private LocalTime horariollegada;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private EstadoReserva estadoReserva;

    @ManyToOne
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @ManyToOne
    @JoinColumn(name = "restaurant_id")
    private Restaurant restaurant;

    /** Espacio elegido por el cliente. Nullable en la base solo por las filas previas al backfill. */
    @ManyToOne
    @JoinColumn(name = "espacio_id")
    private Espacio espacio;
}