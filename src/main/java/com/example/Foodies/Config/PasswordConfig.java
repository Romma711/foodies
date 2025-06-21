package com.example.Foodies.Config;

import com.example.Foodies.Enums.Role;
import com.example.Foodies.Usuario.Usuario;
import com.example.Foodies.Usuario.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class PasswordConfig {

    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CommandLineRunner initAdmin(UsuarioRepository usuarioRepo, PasswordEncoder passwordEncoder) {
        return args -> {
            String emailAdmin = "admin@foodies.com";

            if (!usuarioRepo.existsByEmail(emailAdmin)) {
                Usuario admin = new Usuario();
                admin.setEmail(emailAdmin);
                admin.setPassword(passwordEncoder.encode("admin123")); // contraseña encriptada
                admin.setRol(Role.ROLE_ADMIN);
                admin.setTelefono("1234567890");
                usuarioRepo.save(admin);
                System.out.println("✅ Usuario ADMIN creado");
            } else {
                System.out.println("ℹ️ El usuario ADMIN ya existe");
            }
        };
    }


}
