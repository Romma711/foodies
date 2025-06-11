package com.example.Foodies.EspecialidadDeComida.Dtos;

import com.example.Foodies.Enums.EspecialidadDeComida;

public class EspecialidadDeComidaRequestDTO {

    private EspecialidadDeComida tipoDeComida;

    public EspecialidadDeComida getTipoDeComida() {
        return tipoDeComida;
    }

    public void setTipoDeComida(EspecialidadDeComida tipoDeComida) {
        this.tipoDeComida = tipoDeComida;
    }
}
