package com.example.Foodies.Cliente;

import com.example.Foodies.Cliente.dtos.ClienteDetailDTO;
import com.example.Foodies.Cliente.dtos.ClienteListDTO;
import com.example.Foodies.Cliente.dtos.ClientePatchDTO;
import com.example.Foodies.Cliente.dtos.ClienteRequestDTO;
import com.example.Foodies.Config.PasswordConfig;
import com.example.Foodies.Enums.Role;
import com.example.Foodies.Usuario.Usuario;
import com.example.Foodies.Usuario.UsuarioRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClienteService {
    @Autowired
    private ClienteRepository clienteRepo;

    @Autowired
    private UsuarioRepository usuarioRepo;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Transactional
    public ClienteDetailDTO createCliente (ClienteRequestDTO entrante){
        if(usuarioRepo.existsByEmail(entrante.getUsuario().getEmail())){
            throw new RuntimeException("ERROR: El email ya existe");
        }
        Cliente cliente = clienteRepo.save(new Cliente(entrante.getNombre(), entrante.getApellido(),
                new Usuario(entrante.getUsuario().getEmail(),
                        passwordEncoder.encode(entrante.getUsuario().getPassword()),
                        entrante.getUsuario().getTelefono(),
                        Role.ROLE_CLIENTE)
            )
        );
        return new ClienteDetailDTO(cliente.getUsuario().getId(),
                cliente.getId(),
                cliente.getUsuario().getEmail(),
                cliente.getNombre(),
                cliente.getApellido(),
                cliente.getUsuario().getTelefono());
    }

    public List<ClienteListDTO> getAllClientes() {
        return clienteRepo.findAll().stream()
                .map(v -> new ClienteListDTO(
                        v.getNombre(),v.getApellido(),
                        v.getUsuario().getEmail(),
                        v.getUsuario().getTelefono()
                ))
                .toList();
    }

    public ClienteDetailDTO getById(Long id){
        Cliente cliente = clienteRepo.findById(id).orElseThrow(()-> new RuntimeException("El cliente no existe"));
        return new ClienteDetailDTO(cliente.getUsuario().getId(),
                cliente.getId(),
                cliente.getUsuario().getEmail(),
                cliente.getNombre(),
                cliente.getApellido(),
                cliente.getUsuario().getTelefono());
    }

    public ClienteDetailDTO updateCliente(Long id, ClientePatchDTO update){
        Cliente existente = clienteRepo.findById(id).orElseThrow(()-> new RuntimeException("ERROR: el cliente  no existe"));
        existente.setNombre(update.getNombre());
        existente.setApellido(update.getApellido());
        existente.getUsuario().setTelefono(update.getTelefono());
        clienteRepo.save(existente);
        return new ClienteDetailDTO(existente.getUsuario().getId(),
                existente.getId(),
                existente.getUsuario().getEmail(),
                existente.getNombre(),
                existente.getApellido(),
                existente.getUsuario().getTelefono());
    }

    public void deleteCliente(Long id){
        clienteRepo.deleteById(id);
    }
}
