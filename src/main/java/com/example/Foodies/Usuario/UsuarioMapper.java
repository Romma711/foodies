package com.example.Foodies.Usuario;

import com.example.Foodies.Usuario.dtos.UsuarioDetailDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
@Mapper(componentModel = "spring")
public interface UsuarioMapper {

    @Mapping(source = "rol",target = "rol",qualifiedByName = "rolToString")
    UsuarioDetailDTO toDTO(Usuario usuario);

}
