package com.example.Foodies.Usuario;

import com.example.Foodies.Cliente.ClienteService;
import com.example.Foodies.Cliente.dtos.ClienteDetailDTO;
import com.example.Foodies.Cliente.dtos.ClienteRequestDTO;
import com.example.Foodies.Registro.RegistroRestauranteRequestDTO;
import com.example.Foodies.Usuario.dtos.LoginRequestDTO;
import com.example.Foodies.Usuario.dtos.UsuarioDetailDTO;
import com.example.Foodies.Usuario.dtos.UsuarioRequestDTO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class UsuarioController {
    @Autowired
    private UsuarioService usuarioService;
    @Autowired
    private ClienteService clienteService;

    @PostMapping("/login")
    public String handleLogin(@Valid @RequestBody LoginRequestDTO usuario){
        return usuarioService.login(usuario.getEmail(), usuario.getPassword());
    }

    @PostMapping("/register/cliente")
    public ClienteDetailDTO handleRegisterCliente(@Valid @RequestBody ClienteRequestDTO usuario){
      return clienteService.createCliente(usuario);
    }

    @PostMapping("/register/restaurant")
    public UsuarioDetailDTO handleRegisterRestaurant(@Valid @RequestBody RegistroRestauranteRequestDTO usuario){
        return usuarioService.peticionRegistroRestaurante(usuario);
    }
}
