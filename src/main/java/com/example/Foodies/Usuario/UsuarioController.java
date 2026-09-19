package com.example.Foodies.Usuario;

import com.example.Foodies.Config.JwtUtil;
import com.example.Foodies.Restaurant.Dtos.RegistroRestauranteRequestDTO;
import com.example.Foodies.Usuario.dtos.LoginRequestDTO;
import com.example.Foodies.Usuario.dtos.RegistroClienteDTO;
import com.example.Foodies.Usuario.dtos.UsuarioDetailDTO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class UsuarioController {
    @Autowired
    private UsuarioService usuarioService;

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> handleLogin(@Valid @RequestBody LoginRequestDTO usuario){
        String token = usuarioService.login(usuario.getEmail(), usuario.getPassword());
        JwtUtil.printTokenInfo(token);
        Map<String, String> devolver = new HashMap<>();
        devolver.put("Token","Bearer " +token);
        return ResponseEntity.ok(devolver);
    }

    @PostMapping("/register/cliente")
    public ResponseEntity<UsuarioDetailDTO> handleRegisterCliente(@Valid @RequestBody RegistroClienteDTO usuario){
        UsuarioDetailDTO devolver = usuarioService.registerCliente(usuario);
        return ResponseEntity.created(URI.create("/api/usuarios/"+devolver.id())).body(devolver);
    }

    @PostMapping("/register/restaurante")
    public ResponseEntity<UsuarioDetailDTO> handleRegisterRestaurant(@Valid @RequestBody RegistroRestauranteRequestDTO usuario){
        UsuarioDetailDTO devolver = usuarioService.peticionRegistroRestaurante(usuario);
        return ResponseEntity.created(URI.create("/api/restaurant/"+ devolver.id())).body(devolver);
    }
}