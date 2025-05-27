package com.example.Foodies.Cliente.dtos;

import com.example.Foodies.Usuario.dtos.UsuarioRequestDTO;

public class ClienteRequestDTO {
    private String nombre;
    private String apellido;
    private UsuarioRequestDTO usuario;

    public ClienteRequestDTO(String nombre, String apellido, UsuarioRequestDTO usuario) {
        this.nombre = nombre;
        this.apellido = apellido;
        this.usuario = usuario;
    }
    public ClienteRequestDTO() {
    }
    public String getNombre() {
        return nombre;
    }
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }
    public String getApellido() {
        return apellido;
    }
    public void setApellido(String apellido) {
        this.apellido = apellido;
    }
    public UsuarioRequestDTO getUsuario() {
        return usuario;
    }
    public void setUsuario(UsuarioRequestDTO usuario) {
        this.usuario = usuario;
    }
}
