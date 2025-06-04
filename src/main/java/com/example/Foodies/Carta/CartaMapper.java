package com.example.Foodies.Carta;

import com.example.Foodies.Carta.Dtos.CartaDetailDTO;
import com.example.Foodies.Carta.Dtos.CartaRequestDTO;
import org.mapstruct.Mapping;

public interface CartaMapper {


    Carta toEntity(CartaRequestDTO dto);

    @Mapping(source = "restaurant.id", target = "restaurantId")
    CartaDetailDTO toDTO(Carta carta);
}
