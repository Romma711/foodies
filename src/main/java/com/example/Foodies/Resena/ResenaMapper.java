package com.example.Foodies.Resena;

import com.example.Foodies.Restaurant.Restaurant;
import com.example.Foodies.Resena.dtos.ResenaDetailDTO;
import com.example.Foodies.Resena.dtos.ResenaListDTO;
import com.example.Foodies.Resena.dtos.ResenaRequestDTO;
import com.example.Foodies.Usuario.Usuario;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ResenaMapper {


    @Mapping(target = "nombreUsuario", source = "usuario.nombre")
    @Mapping(target = "nombreRestaurant", source = "restaurant.nombre")
    ResenaDetailDTO toDto(Resena resena);


    Resena toEntity(ResenaRequestDTO resenaDTO);

    @Mapping(target = "nombreUsuario", source = "usuario.nombre")
    ResenaListDTO toListDto(Resena resena);


    List<ResenaListDTO> toListDtoList(List<Resena> resenas);

    // Métodos auxiliares para construir entidades con solo el ID
    default Usuario usuarioFromId(Long id) {
        if (id == null) return null;
        Usuario usuario = new Usuario();
        usuario.setId(id);
        return usuario;
    }

    default Restaurant restaurantFromId(Long id) {
        if (id == null) return null;
        Restaurant restaurant = new Restaurant();
        restaurant.setId(id);
        return restaurant;
    }
}



