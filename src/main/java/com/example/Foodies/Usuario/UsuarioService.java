package com.example.Foodies.Usuario;

import com.example.Foodies.Config.JwtUtil;
import com.example.Foodies.Enums.EspecialidadDeComida;
import com.example.Foodies.Enums.Role;
import com.example.Foodies.Exception.BusinessException;
import com.example.Foodies.Exception.CredencialesInvalidasException;
import com.example.Foodies.Exception.EmailDuplicadoException;
import com.example.Foodies.Exception.EntityNotFoundException;
import com.example.Foodies.Exception.HistorialAsociadoException;
import com.example.Foodies.Resena.ResenaRepository;
import com.example.Foodies.Reserva.ReservaRepository;
import com.example.Foodies.Restaurant.Dtos.RegistroRestauranteRequestDTO;
import com.example.Foodies.Restaurant.Restaurant;
import com.example.Foodies.Restaurant.RestaurantRepository;
import com.example.Foodies.Usuario.dtos.RegistroClienteDTO;
import com.example.Foodies.Usuario.dtos.UsuarioDetailDTO;
import com.example.Foodies.Usuario.dtos.UsuarioListDTO;
import com.example.Foodies.Usuario.dtos.UsuarioPatchDTO;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private ReservaRepository reservaRepo;
    @Autowired
    private ResenaRepository resenaRepo;
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
        // Mismo mensaje y mismo status exista o no el email: no se filtra que emails estan registrados
        Usuario usuario = usuarioRepo.findByEmail(email).orElse(null);

        if (usuario == null || !passwordEncoder.matches(password, usuario.getPassword())){
            throw new CredencialesInvalidasException("Email o contraseña incorrectos");
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
        verificarAccesoCliente(id);
        Usuario usuario = usuarioRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("El cliente no existe"));
        return usuarioMapper.toDTO(usuario);
    }

    public UsuarioDetailDTO updateCliente(Long id, UsuarioPatchDTO update){
        verificarAccesoCliente(id);
        Usuario existente = usuarioRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("ERROR: el cliente no existe"));

        // PATCH: solo se actualizan los campos enviados (los null no pisan lo existente)
        if (update.getNombre() != null) {
            existente.setNombre(update.getNombre());
        }
        if (update.getApellido() != null) {
            existente.setApellido(update.getApellido());
        }
        if (update.getTelefono() != null) {
            existente.setTelefono(update.getTelefono());
        }
        usuarioRepo.save(existente);
        return usuarioMapper.toDTO(existente);
    }

    public void deleteCliente(Long id){
        verificarAccesoCliente(id);
        Usuario usuario = usuarioRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("El cliente no existe"));

        Restaurant restaurante = usuario.getRestaurant();

        if (restaurante != null) {
            // Una cuenta con restaurante (encargado/pendiente) solo la da de baja el admin
            if (usuarioActual().getRol() != Role.ROLE_ADMIN) {
                throw new AccessDeniedException("Solo un admin puede eliminar una cuenta con restaurante");
            }
            if (reservaRepo.existsByRestaurant_Id(restaurante.getId())
                    || resenaRepo.existsByRestaurant_Id(restaurante.getId())) {
                throw new HistorialAsociadoException("El restaurante tiene reservas o reseñas asociadas, no se puede eliminar");
            }
            // Restaurant es el lado dueño de la FK y tiene CascadeType.ALL:
            // al borrarlo tambien se borra la cuenta del encargado
            restaurantRepo.delete(restaurante);
            return;
        }

        if (reservaRepo.existsByUsuario_Id(id) || resenaRepo.existsByUsuario_Id(id)) {
            throw new HistorialAsociadoException("El cliente tiene reservas o reseñas asociadas, no se puede eliminar");
        }

        usuarioRepo.delete(usuario);
    }

    private void verificarAccesoCliente(Long id) {
        Usuario usuarioActual = usuarioActual();
        if (usuarioActual.getRol() == Role.ROLE_ADMIN) {
            return;
        }
        if (!id.equals(usuarioActual.getId())) {
            throw new AccessDeniedException("No podés acceder a un cliente que no sos vos");
        }
    }

    private Usuario usuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof String email) || email.isBlank()) {
            throw new AccessDeniedException("No autenticado");
        }
        return usuarioRepo.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));
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
        rest.setEspecialidad(parsearEspecialidad(r.getEspecialidadDeComida()));
        rest.setCupoMaximo(r.getCupoMaximo());

        rest.setUsuario(usuario);
        usuario.setRestaurant(rest);
        restaurantRepo.save(rest);


        return usuarioMapper.toDTO(usuario);


    }
    @Transactional
    public UsuarioDetailDTO aprobarEncargado(Long usuarioId){
        Usuario usuario = usuarioRepo.findById(usuarioId)
                .orElseThrow(()-> new EntityNotFoundException("Usuario no encontrado"));
        if(usuario.getRestaurant()== null){
            throw new BusinessException("El usuario no tiene restaurante asociado");
        }

        usuario.setRol(Role.ROLE_ENCARGADO);
        usuario.getRestaurant().setAprobado(true);

        return usuarioMapper.toDTO(usuario);
    }

    public void lanzarError(String mensaje){
        throw new BusinessException(mensaje);
    }

    private EspecialidadDeComida parsearEspecialidad(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new BusinessException("La especialidad de comida es obligatoria");
        }
        try {
            return EspecialidadDeComida.valueOf(valor.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Especialidad inválida: " + valor);
        }
    }


}
