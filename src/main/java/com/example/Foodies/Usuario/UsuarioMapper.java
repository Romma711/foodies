package com.example.Foodies.Usuario;

import com.example.Foodies.Enums.Role;
import com.example.Foodies.Usuario.dtos.UsuarioDetailDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface UsuarioMapper {

    @Mapping(source = "rol",target = "rol",qualifiedByName = "rolToString")
    UsuarioDetailDTO toDTO(Usuario usuario);

    @Named("rolToString")
    default String rolToString(Role rol) {
        return rol != null ? rol.name() : null;
    }

}
