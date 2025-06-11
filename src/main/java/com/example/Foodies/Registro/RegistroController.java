package com.example.Foodies.Registro;

import com.example.Foodies.Usuario.UsuarioService;
import com.example.Foodies.Usuario.dtos.UsuarioDetailDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/registro")
public class RegistroController {

    @Autowired
    private UsuarioService usuarioService;


    @PostMapping("/restaurante")
    public ResponseEntity<UsuarioDetailDTO> registrarRestaurante(@RequestBody RegistroRestauranteRequestDTO req){
        UsuarioDetailDTO dto = usuarioService.peticionRegistroRestaurante(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }
}
