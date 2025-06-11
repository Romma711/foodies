package com.example.Foodies.EspecialidadDeComida;

import com.example.Foodies.Restaurant.Restaurant;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Table(name = "Especialidad_De_Comida")
@Entity
public class EspecialidadDeComida {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private com.example.Foodies.Enums.EspecialidadDeComida tipoDeComida;


    @OneToMany(mappedBy = "especialidad")
    private List<Restaurant> restaurantes = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public com.example.Foodies.Enums.EspecialidadDeComida getTipoDeComida() {
        return tipoDeComida;
    }

    public void setTipoDeComida(com.example.Foodies.Enums.EspecialidadDeComida tipoDeComida) {
        this.tipoDeComida = tipoDeComida;
    }
}
