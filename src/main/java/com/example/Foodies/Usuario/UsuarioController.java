package com.example.Foodies.Usuario;

import com.example.Foodies.Cliente.ClienteService;
import com.example.Foodies.Cliente.dtos.ClienteDetailDTO;
import com.example.Foodies.Cliente.dtos.ClienteRequestDTO;
import com.example.Foodies.Config.JwtUtil;
import com.example.Foodies.Exception.BusinessException;
import com.example.Foodies.Registro.RegistroRestauranteRequestDTO;
import com.example.Foodies.Usuario.dtos.LoginRequestDTO;
import com.example.Foodies.Usuario.dtos.UsuarioDetailDTO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class UsuarioController {
    @Autowired
    private UsuarioService usuarioService;
    @Autowired
    private ClienteService clienteService;

    @PostMapping("/login")
    public String handleLogin(@Valid @RequestBody LoginRequestDTO usuario){
        String token = usuarioService.login(usuario.getEmail(), usuario.getPassword());
        JwtUtil.printTokenInfo(token);
        return token;
    }

    @PostMapping("/register/cliente")
    public ClienteDetailDTO handleRegisterCliente(@Valid @RequestBody ClienteRequestDTO usuario){
      return clienteService.createCliente(usuario);
    }

    @PostMapping("/register/restaurant")
    public UsuarioDetailDTO handleRegisterRestaurant(@Valid @RequestBody RegistroRestauranteRequestDTO usuario){
        return usuarioService.peticionRegistroRestaurante(usuario);
    }
    @GetMapping("/test")
    public ResponseEntity<?> testHandler() {
        usuarioService.lanzarError("Error de prueba");
        return ResponseEntity.ok("OK");
    }

}
