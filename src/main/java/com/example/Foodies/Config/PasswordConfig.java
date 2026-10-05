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
            // Se exige ADMIN_PASSWORD siempre: nunca se arranca con una contraseña default conocida
            String password = cargarPasswordAdmin();

            if (!usuarioRepo.existsByEmail(emailAdmin)) {
                Usuario admin = new Usuario();
                admin.setEmail(emailAdmin);
                admin.setPassword(passwordEncoder.encode(password)); // contraseña encriptada
                admin.setRol(Role.ROLE_ADMIN);
                admin.setTelefono("1234567890");
                usuarioRepo.save(admin);
                System.out.println("✅ Usuario ADMIN creado");
            } else {
                System.out.println("ℹ️ El usuario ADMIN ya existe");
            }
        };
    }

    private static String cargarPasswordAdmin() {
        String password = System.getProperty("ADMIN_PASSWORD");
        if (password == null || password.isBlank()) {
            password = System.getenv("ADMIN_PASSWORD");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalStateException(
                    "Falta la variable de entorno ADMIN_PASSWORD: definala antes de arrancar la aplicacion "
                            + "(ej: ADMIN_PASSWORD=<contraseña-de-8-caracteres-o-mas> ./mvnw spring-boot:run)");
        }
        if (password.length() < 8) {
            throw new IllegalStateException("ADMIN_PASSWORD debe tener al menos 8 caracteres");
        }
        return password;
    }


}
