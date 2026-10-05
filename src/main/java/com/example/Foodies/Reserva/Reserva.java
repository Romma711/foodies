package com.example.Foodies.Reserva;

import com.example.Foodies.Enums.EstadoReserva;
import com.example.Foodies.Restaurant.Restaurant;
import com.example.Foodies.Usuario.Usuario;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "reserva")
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"usuario", "restaurant"})
@EqualsAndHashCode(exclude = {"usuario", "restaurant"})
public class Reserva {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @NotNull
    private Integer cantidad;

    @NotNull
    private LocalDate fechaReserva;

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
}