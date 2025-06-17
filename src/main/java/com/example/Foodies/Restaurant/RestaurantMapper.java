package com.example.Foodies.Restaurant;
import com.example.Foodies.Restaurant.Dtos.RestaurantDetailDTO;
import com.example.Foodies.Restaurant.Dtos.RestaurantListDTO;
import com.example.Foodies.Restaurant.Dtos.RestaurantPatchDTO;
import com.example.Foodies.Restaurant.Dtos.RestaurantRequestDTO;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;


@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface RestaurantMapper {

    Restaurant toEntity(RestaurantRequestDTO DTO);

    RestaurantDetailDTO toDetailDTO(Restaurant restaurant);

    List<RestaurantListDTO> toListDTO(List<Restaurant> restaurantes);


    void patchRestaurantFromDTO(RestaurantPatchDTO dto, @MappingTarget Restaurant restaurant);


}
