package com.example.Foodies.Usuario;

import com.example.Foodies.Cliente.Cliente;
import com.example.Foodies.Cliente.ClienteRepository;
import com.example.Foodies.Usuario.dtos.UsuarioRequestDTO;
import org.aspectj.lang.reflect.UnlockSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.swing.text.html.Option;
import java.util.List;
import java.util.Optional;

@Service
public class UsuarioService {
    @Autowired
    private UsuarioRepository usuarioRepo;


    public Usuario createCliente (UsuarioRequestDTO nuevo){
        if(usuarioRepo.existsByEmail(nuevo.getEmail())){
            throw new RuntimeException("El email ya existe");
        }

        return usuarioRepo.save(new Usuario(nuevo.getEmail(), nuevo.getPassword(), nuevo.getTelefono()));
    }

    public Usuario getById(Long id){
        return usuarioRepo.findById(id).orElseThrow(() -> new RuntimeException("El usuario no existe"));
    }

}
