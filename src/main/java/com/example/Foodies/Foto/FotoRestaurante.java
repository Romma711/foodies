package com.example.Foodies.Foto;

import com.example.Foodies.Restaurant.Restaurant;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.Instant;

/** La imagen vive en {@link com.example.Foodies.Storage.FileStorage}; esta fila solo guarda su clave. */
@Entity
@Table(name = "FotosRestaurante")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FotoRestaurante {

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
    private TipoFoto tipo;

    @Column(name = "storage_key", nullable = false)
    private String storageKey;

    @Column(name = "creada_en", nullable = false)
    private Instant creadaEn;
}
