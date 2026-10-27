package com.example.Foodies.Resena;

import com.example.Foodies.Exception.BusinessException;
import com.example.Foodies.Exception.ForbiddenException;
import com.example.Foodies.Restaurant.Restaurant;
import com.example.Foodies.Restaurant.RestaurantRepository;
import com.example.Foodies.Resena.dtos.ResenaDetailDTO;
import com.example.Foodies.Resena.dtos.ResenaPatchDTO;
import com.example.Foodies.Resena.dtos.ResenaRequestDTO;
import com.example.Foodies.Usuario.Usuario;
import com.example.Foodies.Usuario.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResenaServiceTest {

    @Mock
    private ResenaRepository resenaRepo;

    @Mock
    private UsuarioRepository usuarioRepo;

    @Mock
    private RestaurantRepository restauranteRepo;

    @Mock
    private ResenaMapper resenaMapper;

    @InjectMocks
    private ResenaService resenaService;

    private Usuario autora;
    private Restaurant restaurante;
    private Resena resena;

    @BeforeEach
    void setUp() {
        autora = new Usuario();
        autora.setId(7L);
        autora.setEmail("ana@foodies.com");
        autora.setNombre("Ana");

        restaurante = new Restaurant();
        restaurante.setId(5L);
        restaurante.setNombre("Ochiwan");

        resena = new Resena();
        resena.setId(1L);
        resena.setUsuario(autora);
        resena.setRestaurant(restaurante);
        resena.setComentario("La pasta estaba increíble.");
        resena.setCalificacion(5);
    }

    @Test
    void creaLaResenaConElUsuarioDelToken() {
        ResenaRequestDTO dto = new ResenaRequestDTO("La pasta estaba increíble.", 5, 5L);
        when(usuarioRepo.findByEmail("ana@foodies.com")).thenReturn(Optional.of(autora));
        when(restauranteRepo.findById(5L)).thenReturn(Optional.of(restaurante));
        when(resenaRepo.existsByUsuario_IdAndRestaurant_Id(7L, 5L)).thenReturn(false);
        when(resenaMapper.toEntity(dto)).thenReturn(new Resena());
        when(resenaRepo.save(any(Resena.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(resenaMapper.toDto(any(Resena.class)))
                .thenReturn(new ResenaDetailDTO(1L, dto.getComentario(), 5, "Ana", "Ochiwan"));

        ResenaDetailDTO resultado = resenaService.createResena(dto, "ana@foodies.com");

        assertThat(resultado.nombreUsuario()).isEqualTo("Ana");
        assertThat(resultado.nombreRestaurant()).isEqualTo("Ochiwan");
        verify(resenaRepo).save(argThat(guardada ->
                guardada.getUsuario() == autora && guardada.getRestaurant() == restaurante));
    }

    @Test
    void rechazaUnaSegundaResenaDelMismoUsuario() {
        ResenaRequestDTO dto = new ResenaRequestDTO("Otra experiencia distinta", 4, 5L);
        when(usuarioRepo.findByEmail("ana@foodies.com")).thenReturn(Optional.of(autora));
        when(restauranteRepo.findById(5L)).thenReturn(Optional.of(restaurante));
        when(resenaRepo.existsByUsuario_IdAndRestaurant_Id(7L, 5L)).thenReturn(true);

        assertThatThrownBy(() -> resenaService.createResena(dto, "ana@foodies.com"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Ya realizaste");

        verify(resenaRepo, never()).save(any(Resena.class));
    }

    @Test
    void rechazaCalificacionesFueraDeRango() {
        ResenaRequestDTO dto = new ResenaRequestDTO("Un comentario suficientemente largo", 6, 5L);
        when(usuarioRepo.findByEmail("ana@foodies.com")).thenReturn(Optional.of(autora));
        when(restauranteRepo.findById(5L)).thenReturn(Optional.of(restaurante));
        when(resenaRepo.existsByUsuario_IdAndRestaurant_Id(7L, 5L)).thenReturn(false);

        assertThatThrownBy(() -> resenaService.createResena(dto, "ana@foodies.com"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("entre 1 y 5");

        verify(resenaRepo, never()).save(any(Resena.class));
    }

    @Test
    void noPermiteEditarLaResenaDeOtroUsuario() {
        when(resenaRepo.findById(1L)).thenReturn(Optional.of(resena));
        ResenaPatchDTO patch = new ResenaPatchDTO(1L, "Un comentario distinto", 4);

        assertThatThrownBy(() ->
                resenaService.updateResena(1L, patch, "otro@foodies.com", false))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("otro usuario");

        verify(resenaRepo, never()).save(any(Resena.class));
    }

    @Test
    void permiteEditarAlAutor() {
        when(resenaRepo.findById(1L)).thenReturn(Optional.of(resena));
        when(resenaRepo.save(resena)).thenReturn(resena);
        ResenaPatchDTO patch = new ResenaPatchDTO(1L, "Comentario actualizado", 4);

        ResenaDetailDTO resultado = resenaService.updateResena(
                1L,
                patch,
                "ANA@foodies.com",
                false
        );

        assertThat(resultado.comentario()).isEqualTo("Comentario actualizado");
        assertThat(resultado.calificacion()).isEqualTo(4);
    }

    @Test
    void noPermiteEliminarLaResenaDeOtroUsuario() {
        when(resenaRepo.findById(1L)).thenReturn(Optional.of(resena));

        assertThatThrownBy(() ->
                resenaService.deleteResena(1L, "otro@foodies.com", false))
                .isInstanceOf(ForbiddenException.class);

        verify(resenaRepo, never()).delete(any(Resena.class));
    }

    @Test
    void permiteEliminarAlAdmin() {
        when(resenaRepo.findById(1L)).thenReturn(Optional.of(resena));

        resenaService.deleteResena(1L, "admin@foodies.com", true);

        verify(resenaRepo).delete(resena);
    }
}
