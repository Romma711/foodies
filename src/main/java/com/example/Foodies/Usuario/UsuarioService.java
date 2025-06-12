package com.example.Foodies.Usuario;

import com.example.Foodies.Enums.Role;
import com.example.Foodies.Exception.EmailDuplicadoException;
import com.example.Foodies.Exception.EntityNotFoundException;
import com.example.Foodies.Registro.RegistroRestauranteRequestDTO;
import com.example.Foodies.Restaurant.Restaurant;
import com.example.Foodies.Usuario.dtos.UsuarioDetailDTO;
import com.example.Foodies.Usuario.dtos.UsuarioRequestDTO;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UsuarioService implements UserDetailsService {
    @Autowired
    private UsuarioRepository usuarioRepo;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private UsuarioMapper usuarioMapper;


    public Usuario createCliente (UsuarioRequestDTO nuevo){
        if(usuarioRepo.existsByEmail(nuevo.getEmail())){
            throw new RuntimeException("El email ya existe");
        }

        return usuarioRepo.save(new Usuario(nuevo.getEmail(), nuevo.getPassword(), nuevo.getTelefono()));
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepo.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

        return new User(
                usuario.getEmail(),
                usuario.getPassword(),
                List.of(new SimpleGrantedAuthority(usuario.getRol().toString()))
        );
    }

    public Usuario getById(Long id){
        return usuarioRepo.findById(id).orElseThrow(() -> new RuntimeException("El usuario no existe"));
    }
    @Transactional
    public UsuarioDetailDTO peticionRegistroRestaurante(RegistroRestauranteRequestDTO r){
        if(usuarioRepo.existsByEmail(r.getEmail())){
            throw new EmailDuplicadoException("El email esta en uso");
        }
        Usuario usuario = new Usuario();
        usuario.setEmail(r.getEmail());
        usuario.setPassword(passwordEncoder.encode(r.getPassword()));
        usuario.setTelefono(r.getTelefono());
        usuario.setRol(Role.ROLE_CLIENTE);

        Restaurant rest = new Restaurant();
        rest.setNombre(r.getNombreRestaurante());
        rest.setUbicacion(r.getDireccion());
        rest.setAprobado(false);
        rest.setEspecialidad(r.getEspecialidadDeComida());
        rest.setCupoMaximo(r.getCupoMaximo());

        usuario.setRestaurant(rest);
        usuarioRepo.save(usuario);

        return usuarioMapper.toDTO(usuario);


    }
    @Transactional
    public UsuarioDetailDTO aprobarEncargado(Long usuarioId){
        Usuario usuario = usuarioRepo.findById(usuarioId)
                .orElseThrow(()-> new EntityNotFoundException("Usuario no encontrado"));
        if(usuario.getRestaurant()== null){
            throw new IllegalStateException("Este usuario no tiene restaurante asociado");
        }

        usuario.setRol(Role.ROLE_ENCARGADO);
        usuario.getRestaurant().setAprobado(true);

        return usuarioMapper.toDTO(usuario);
    }



}
