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

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
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

    /** Stored as varchar (not a MySQL enum) so new states can be added; see {@link EstadoReservaMigracion}. */
    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
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

    /** {@code fechaReserva} as a date; JPA may return {@code java.sql.Date}/{@code Timestamp}. */
    public LocalDate fechaLocal() {
        if (fechaReserva == null) {
            return null;
        }
        if (fechaReserva instanceof java.sql.Date sql) {
            return sql.toLocalDate();
        }
        return new Date(fechaReserva.getTime()).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    /** Date and arrival time together. */
    public LocalDateTime llegada() {
        LocalDate fecha = fechaLocal();
        return fecha == null || horariollegada == null ? null : LocalDateTime.of(fecha, horariollegada);
    }
}
