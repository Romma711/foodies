package com.example.Foodies.Carta;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "Cartas")
public class Carta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El archivo debe tener un nombre")
    @Column(nullable = false)
    private String nombreArchivo;
}
