package com.example.Foodies.Carta;

import com.example.Foodies.Restaurant.Restaurant;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * The PDF itself lives in {@link com.example.Foodies.Storage.FileStorage}; this row only keeps its key.
 * The old `contenido_pdf` column is intentionally left unmapped (not dropped) so existing data is not destroyed.
 */
@Entity
@Table(name = "Cartas")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Carta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El archivo debe tener un nombre")
    @Column(nullable = false)
    private String nombreArchivo;

    /** Null only for legacy rows created when PDFs were stored in the database. */
    @Column(name = "storage_key")
    private String storageKey;

    @Column(name = "tamanio_bytes")
    private Long tamanioBytes;

    @Column(name = "actualizada_en")
    private Instant actualizadaEn;

    @OneToOne
    @JoinColumn(name = "restaurant_id", nullable = false, unique = true)
    private Restaurant restaurant;

}
