package com.example.Foodies.Usuario;

import com.example.Foodies.Enums.Role;
import com.example.Foodies.Usuario.dtos.UsuarioDetailDTO;
import com.example.Foodies.Usuario.dtos.UsuarioListDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UsuarioMapper {

    @Mapping(source = "rol",target = "rol",qualifiedByName = "rolToString")
    UsuarioDetailDTO toDTO(Usuario usuario);

    @Mapping(source = "rol",target = "rol",qualifiedByName = "rolToString")
    UsuarioListDTO toListDTO(Usuario usuario);

    List<UsuarioListDTO> toListDTO(List<Usuario> usuarios);

    @Named("rolToString")
    default String rolToString(Role rol) {
        return rol != null ? rol.name() : null;
    }

}