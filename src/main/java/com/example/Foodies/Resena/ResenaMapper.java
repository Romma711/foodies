package com.example.Foodies.Resena;

import com.example.Foodies.Restaurant.Restaurant;
import com.example.Foodies.Resena.dtos.ResenaDetailDTO;
import com.example.Foodies.Resena.dtos.ResenaListDTO;
import com.example.Foodies.Resena.dtos.ResenaRequestDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ResenaMapper {

    @Mapping(target = "nombreUsuario", source = "usuario.nombre")
    @Mapping(target = "nombreRestaurant", source = "restaurant.nombre")
    ResenaDetailDTO toDto(Resena resena);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "usuario", ignore = true)
    @Mapping(target = "restaurant", source = "restaurantId")
    Resena toEntity(ResenaRequestDTO resenaDTO);

    @Mapping(target = "nombreUsuario", source = "usuario.nombre")
    ResenaListDTO toListDto(Resena resena);

    List<ResenaListDTO> toListDtoList(List<Resena> resenas);

    default Restaurant restaurantFromId(Long id) {
        if (id == null) {
            return null;
        }
        Restaurant restaurant = new Restaurant();
        restaurant.setId(id);
        return restaurant;
    }
}
