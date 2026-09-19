package com.example.Foodies.Usuario;

import com.example.Foodies.Config.JwtUtil;
import com.example.Foodies.Enums.EspecialidadDeComida;
import com.example.Foodies.Enums.Role;
import com.example.Foodies.Exception.BusinessException;
import com.example.Foodies.Exception.EmailDuplicadoException;
import com.example.Foodies.Exception.EntityNotFoundException;
import com.example.Foodies.Exception.NotApprovedException;
import com.example.Foodies.Restaurant.Dtos.RegistroRestauranteRequestDTO;
import com.example.Foodies.Restaurant.Restaurant;
import com.example.Foodies.Restaurant.RestaurantRepository;
import com.example.Foodies.Usuario.dtos.RegistroClienteDTO;
import com.example.Foodies.Usuario.dtos.UsuarioDetailDTO;
import com.example.Foodies.Usuario.dtos.UsuarioListDTO;
import com.example.Foodies.Usuario.dtos.UsuarioPatchDTO;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
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
    private RestaurantRepository restaurantRepo;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private UsuarioMapper usuarioMapper;

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
    @Transactional
    public String login (String email, String password){
        Usuario usuario = usuarioRepo.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

        if(!passwordEncoder.matches(password,usuario.getPassword())){
            throw new NotApprovedException("Email o contraseña incorrectos, intente nuevamente");
        }

        UserDetails userDetails = loadUserByUsername(email);
        return JwtUtil.createToken(userDetails.getUsername(),userDetails.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList());
    }


    public Usuario getById(Long id){
        return usuarioRepo.findById(id).orElseThrow(() -> new RuntimeException("El usuario no existe"));
    }

    @Transactional
    public UsuarioDetailDTO registerCliente(RegistroClienteDTO entrante){
        if(usuarioRepo.existsByEmail(entrante.getEmail())){
            throw new EmailDuplicadoException("ERROR: El email ya existe");
        }
        Usuario usuario = new Usuario();
        usuario.setNombre(entrante.getNombre());
        usuario.setApellido(entrante.getApellido());
        usuario.setEmail(entrante.getEmail());
        usuario.setPassword(passwordEncoder.encode(entrante.getPassword()));
        usuario.setTelefono(entrante.getTelefono());
        usuario.setRol(Role.ROLE_CLIENTE);
        usuarioRepo.save(usuario);
        return usuarioMapper.toDTO(usuario);
    }

    public List<UsuarioListDTO> getAllClientes() {
        return usuarioMapper.toListDTO(usuarioRepo.findByRol(Role.ROLE_CLIENTE));
    }

    public UsuarioDetailDTO getClienteById(Long id){
        Usuario usuario = usuarioRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("El cliente no existe"));
        return usuarioMapper.toDTO(usuario);
    }

    public UsuarioDetailDTO updateCliente(Long id, UsuarioPatchDTO update){
        Usuario existente = usuarioRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("ERROR: el cliente no existe"));
        existente.setNombre(update.getNombre());
        existente.setApellido(update.getApellido());
        existente.setTelefono(update.getTelefono());
        usuarioRepo.save(existente);
        return usuarioMapper.toDTO(existente);
    }

    public void deleteCliente(Long id){
        usuarioRepo.deleteById(id);
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
        usuario.setRol(Role.ROLE_PENDIENTE);

        Restaurant rest = new Restaurant();
        rest.setNombre(r.getNombreRestaurante());
        rest.setUbicacion(r.getDireccion());
        rest.setAprobado(false);
        rest.setEspecialidad(EspecialidadDeComida.valueOf(r.getEspecialidadDeComida()));
        rest.setCupoMaximo(r.getCupoMaximo());

        rest.setUsuario(usuario);
        restaurantRepo.save(rest);


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

    public void lanzarError(String mensaje){
        throw new BusinessException(mensaje);
    }


}
