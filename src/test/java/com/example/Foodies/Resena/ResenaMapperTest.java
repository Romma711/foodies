package com.example.Foodies.Resena;

import com.example.Foodies.Resena.dtos.ResenaUsuarioDTO;
import com.example.Foodies.Restaurant.Restaurant;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ResenaMapperTest {

    private final ResenaMapper mapper = new ResenaMapperImpl();

    @Test
    void lasResenasDelUsuarioTraenElRestauranteResenado() {
        Restaurant restaurante = new Restaurant();
        restaurante.setId(5L);
        restaurante.setNombre("Ochiwan");
        Resena resena = new Resena();
        resena.setId(9L);
        resena.setComentario("Muy rico");
        resena.setCalificacion(4);
        resena.setRestaurant(restaurante);

        List<ResenaUsuarioDTO> dtos = mapper.toUsuarioDtoList(List.of(resena));

        assertThat(dtos).containsExactly(new ResenaUsuarioDTO(9L, "Muy rico", 4, 5L, "Ochiwan"));
    }
}
