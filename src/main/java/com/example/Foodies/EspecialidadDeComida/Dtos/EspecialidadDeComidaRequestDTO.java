package com.example.Foodies.EspecialidadDeComida.Dtos;

import com.example.Foodies.Enums.EspecialidadDeComida;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EspecialidadDeComidaRequestDTO {

    private EspecialidadDeComida tipoDeComida;
}