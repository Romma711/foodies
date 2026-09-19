package com.example.Foodies.Usuario;

import com.example.Foodies.Enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);
    Boolean existsByEmail(String email);
    Boolean existsByEmailAndPassword(String email, String password);

    List<Usuario> findByRol(Role rol);
}
