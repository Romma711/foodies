package com.example.Foodies.Usuario;

import com.example.Foodies.Cliente.ClienteService;
import com.example.Foodies.Cliente.dtos.ClienteDetailDTO;
import com.example.Foodies.Cliente.dtos.ClienteRequestDTO;
import com.example.Foodies.Config.JwtUtil;
import com.example.Foodies.Exception.BusinessException;
import com.example.Foodies.Registro.RegistroRestauranteRequestDTO;
import com.example.Foodies.Restaurant.Restaurant;
import com.example.Foodies.Usuario.dtos.LoginRequestDTO;
import com.example.Foodies.Usuario.dtos.UsuarioDetailDTO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class UsuarioController {
    @Autowired
    private UsuarioService usuarioService;
    @Autowired
    private ClienteService clienteService;

    @PostMapping("/login")
    public ResponseEntity<String> handleLogin(@Valid @RequestBody LoginRequestDTO usuario){
        String token = usuarioService.login(usuario.getEmail(), usuario.getPassword());
        JwtUtil.printTokenInfo(token);
        return ResponseEntity.ok("Bearer " +token);
    }

    @PostMapping("/register/cliente")
    public ResponseEntity<ClienteDetailDTO> handleRegisterCliente(@Valid @RequestBody ClienteRequestDTO usuario){
        ClienteDetailDTO devolver = clienteService.createCliente(usuario);
        return ResponseEntity.created(URI.create("/api/cliente/"+devolver.idCliente())).body(devolver);
    }

    @PostMapping("/register/restaurant")
    public ResponseEntity<UsuarioDetailDTO> handleRegisterRestaurant(@Valid @RequestBody RegistroRestauranteRequestDTO usuario){
        UsuarioDetailDTO devolver = usuarioService.peticionRegistroRestaurante(usuario);
        return ResponseEntity.created(URI.create("/api/restaurant/"+ devolver.id())).body(devolver);
    }
    @GetMapping("/test")
    public ResponseEntity<?> testHandler() {
        usuarioService.lanzarError("Error de prueba");
        return ResponseEntity.ok("OK");
    }

}
