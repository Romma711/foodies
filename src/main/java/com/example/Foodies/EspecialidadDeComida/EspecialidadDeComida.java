package com.example.Foodies.EspecialidadDeComida;

import com.example.Foodies.Enums.TipoDeComida;
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
    private TipoDeComida tipoDeComida;


    @OneToMany(mappedBy = "especialidad")
    private List<Restaurant> restaurantes = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TipoDeComida getTipoDeComida() {
        return tipoDeComida;
    }

    public void setTipoDeComida(TipoDeComida tipoDeComida) {
        this.tipoDeComida = tipoDeComida;
    }
}
