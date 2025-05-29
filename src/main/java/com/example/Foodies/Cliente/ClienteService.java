package com.example.Foodies.Cliente;

import com.example.Foodies.Cliente.dtos.ClienteDetailDTO;
import com.example.Foodies.Cliente.dtos.ClienteListDTO;
import com.example.Foodies.Cliente.dtos.ClienteRequestDTO;
import com.example.Foodies.Usuario.Usuario;
import com.example.Foodies.Usuario.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClienteService {
    @Autowired
    private ClienteRepository clienteRepo;

    @Autowired
    private UsuarioRepository usuarioRepo;

    public ClienteDetailDTO createCliente (ClienteRequestDTO entrante){
        if(usuarioRepo.existsByEmail(entrante.getUsuario().getEmail())){
            throw new RuntimeException("ERROR: El email ya existe");
        }
        Cliente cliente = clienteRepo.save(new Cliente(entrante.getNombre(), entrante.getApellido(), usuarioRepo.save(
                new Usuario(entrante.getUsuario().getEmail(),
                        entrante.getUsuario().getPassword(),
                        entrante.getUsuario().getTelefono())
                )
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
}
