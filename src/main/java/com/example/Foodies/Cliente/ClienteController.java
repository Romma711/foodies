package com.example.Foodies.Cliente;

import com.example.Foodies.Cliente.dtos.ClienteDetailDTO;
import com.example.Foodies.Cliente.dtos.ClienteListDTO;
import com.example.Foodies.Cliente.dtos.ClientePatchDTO;
import com.example.Foodies.Cliente.dtos.ClienteRequestDTO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cliente")
public class ClienteController {
    @Autowired
    private ClienteService clienteService;

    @GetMapping("/list")
    public ResponseEntity<List<ClienteListDTO>> handleGetAllClientes(){
        return ResponseEntity.ok(clienteService.getAllClientes());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClienteDetailDTO> handleGetById(@PathVariable Long id){
        return ResponseEntity.ok(clienteService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClienteDetailDTO> handleUpdateById(@PathVariable Long id, @RequestBody ClientePatchDTO update){
        return ResponseEntity.ok(clienteService.updateCliente(id, update));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> handleDeleteById(@PathVariable Long id){
        clienteService.deleteCliente(id);
        return ResponseEntity.noContent().build();
    }
}
