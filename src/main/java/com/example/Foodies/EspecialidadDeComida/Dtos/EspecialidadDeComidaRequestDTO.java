package com.example.Foodies.EspecialidadDeComida.Dtos;

import com.example.Foodies.Enums.TipoDeComida;

public class EspecialidadDeComidaRequestDTO {

    private TipoDeComida tipoDeComida;

    public TipoDeComida getTipoDeComida() {
        return tipoDeComida;
    }

    public void setTipoDeComida(TipoDeComida tipoDeComida) {
        this.tipoDeComida = tipoDeComida;
    }
}
