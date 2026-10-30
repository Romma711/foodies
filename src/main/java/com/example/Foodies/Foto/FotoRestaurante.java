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

    /** Exactamente una foto por restaurante es la portada: la miniatura del catálogo. */
    @Column(nullable = false)
    private boolean portada;

    @Column(name = "storage_key", nullable = false)
    private String storageKey;

    @Column(name = "creada_en", nullable = false)
    private Instant creadaEn;
}
