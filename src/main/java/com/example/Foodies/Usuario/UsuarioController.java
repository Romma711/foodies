package com.example.Foodies.Usuario;

import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
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

    /** Multipart: `datos` (JSON), `fotos` (3 a 5) y `portada` (índice en `fotos` de la que se muestra en el catálogo). */
    @PostMapping(value = "/register/restaurante", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UsuarioDetailDTO> handleRegisterRestaurant(
            @Valid @RequestPart("datos") RegistroRestauranteRequestDTO usuario,
            @RequestPart(value = "fotos", required = false) List<MultipartFile> fotos,
            @RequestParam(value = "portada", required = false) Integer portada){
        UsuarioDetailDTO devolver = usuarioService.peticionRegistroRestaurante(usuario, fotos, portada);
        return ResponseEntity.created(URI.create("/api/restaurant/"+ devolver.id())).body(devolver);
    }
}