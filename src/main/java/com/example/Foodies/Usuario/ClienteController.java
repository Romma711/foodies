package com.example.Foodies.Usuario;

import com.example.Foodies.Usuario.dtos.UsuarioDetailDTO;
import com.example.Foodies.Usuario.dtos.UsuarioListDTO;
import com.example.Foodies.Usuario.dtos.UsuarioPatchDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/clientes")
public class ClienteController {

    @Autowired
    private UsuarioService usuarioService;

    /** ?page=0&size=20&sort=nombre,asc */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<Page<UsuarioListDTO>> handleGetAllClientes(Pageable pageable){
        return ResponseEntity.ok(usuarioService.getAllClientes(pageable));
    }

    @PreAuthorize("hasAnyRole('CLIENTE', 'ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<UsuarioDetailDTO> handleGetById(@PathVariable Long id){
        return ResponseEntity.ok(usuarioService.getClienteById(id));
    }

    @PreAuthorize("hasAnyRole('CLIENTE', 'ADMIN')")
    @PatchMapping("/{id}")
    public ResponseEntity<UsuarioDetailDTO> handleUpdateById(@PathVariable Long id, @RequestBody UsuarioPatchDTO update){
        return ResponseEntity.ok(usuarioService.updateCliente(id, update));
    }

    @PreAuthorize("hasAnyRole('CLIENTE', 'ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> handleDeleteById(@PathVariable Long id){
        usuarioService.deleteCliente(id);
        return ResponseEntity.noContent().build();
    }
}