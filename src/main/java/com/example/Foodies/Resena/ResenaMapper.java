package com.example.Foodies.Resena;

import com.example.Foodies.Cliente.Cliente;
import com.example.Foodies.Resena.dtos.ResenaDetailDTO;
import com.example.Foodies.Resena.dtos.ResenaListDTO;
import com.example.Foodies.Resena.dtos.ResenaRequestDTO;
import com.example.Foodies.Restaurant.Restaurant;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ResenaMapper {


    @Mapping(target = "nombreCliente", source = "cliente.nombre")
    @Mapping(target = "nombreRestaurant", source = "restaurant.nombre")
    ResenaDetailDTO toDto(Resena resena);


    Resena toEntity(ResenaRequestDTO resenaDTO);

    @Mapping(target = "nombreCliente", source = "cliente.nombre")
    ResenaListDTO toListDto(Resena resena);


    List<ResenaListDTO> toListDtoList(List<Resena> resenas);

    // Métodos auxiliares para construir entidades con solo el ID
    default Cliente clienteFromId(Long id) {
        if (id == null) return null;
        Cliente cliente = new Cliente();
        cliente.setId(id);
        return cliente;
    }

    default Restaurant restaurantFromId(Long id) {
        if (id == null) return null;
        Restaurant restaurant = new Restaurant();
        restaurant.setId(id);
        return restaurant;
    }
}



