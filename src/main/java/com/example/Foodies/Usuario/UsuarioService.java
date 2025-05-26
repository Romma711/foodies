package com.example.Foodies.Usuario;

import com.example.Foodies.Cliente.Cliente;
import com.example.Foodies.Cliente.ClienteRepository;
import org.aspectj.lang.reflect.UnlockSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UsuarioService {
    @Autowired
    private UsuarioRepository usuarioRepo;
    private ClienteRepository clienteRepo;


    public Usuario createCliente (Usuario nuevo, Cliente cliente){
        if(usuarioRepo.existsByEmail(nuevo.getEmail())){
            throw new RuntimeException("El email ya existe");
        }
        return usuarioRepo.save(nuevo);
    }

    public List<Usuario> getAll(){
        return usuarioRepo.findAll();
    }

}
