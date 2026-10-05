package com.example.Foodies.Carta;

import com.example.Foodies.Restaurant.Restaurant;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "Cartas")
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"restaurant"})
@EqualsAndHashCode(exclude = {"restaurant"})
public class Carta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El archivo debe tener un nombre")
    @Column(nullable = false)
    private String nombreArchivo;


    @Lob
    @Column(name = "contenido_pdf", columnDefinition = "LONGBLOB")
    private byte[] contenidoPdf;

    @OneToOne
    @JoinColumn(name = "restaurant_id", nullable = false, unique = true)
    private Restaurant restaurant;

}