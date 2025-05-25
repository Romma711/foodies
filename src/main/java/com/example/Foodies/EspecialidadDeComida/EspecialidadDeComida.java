package com.example.Foodies.EspecialidadDeComida;

import com.example.Foodies.Enums.TipoDeComida;
import jakarta.persistence.*;

@Table(name = "Especialidad_De_Comida")
@Entity
public class EspecialidadDeComida {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TipoDeComida tipoDeComida;


}
