package com.example.Foodies.ADMIN;

import com.example.Foodies.Usuario.UsuarioService;
import com.example.Foodies.Usuario.dtos.UsuarioDetailDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminRestauranteController {

    @Autowired
    private UsuarioService usuarioService;


    @PutMapping("/{usuarioid}/aprobar")
    public ResponseEntity<UsuarioDetailDTO> aprobarEncargado(@PathVariable Long usuarioID){
        UsuarioDetailDTO dto = usuarioService.aprobarEncargado(usuarioID);
        return ResponseEntity.ok(dto);
    }
}
