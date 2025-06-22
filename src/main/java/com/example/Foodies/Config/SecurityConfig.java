package com.example.Foodies.Config;

import com.example.Foodies.Enums.Role;
import com.example.Foodies.Usuario.Usuario;
import com.example.Foodies.Usuario.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/reseñas",
                                "/api/carta/*/descargar",
                                "/api/auth/login",
                                "/api/auth/register/cliente",
                                "/api/auth/register/restaurant",
                                "/api/cliente/create",
                                "/api/registro/restaurante",
                                "/api/auth/**",
                                "/api/restaurante/all",
                                "/api/restaurante/especialidad",
                                "/api/restaurante/{id}",
                                "/error"  // <-- Agregado para permitir acceso a la página de error
                        ).permitAll()
                        .requestMatchers(
                                "/api/reserva/create",
                                "/api/cliente/update",
                                "/api/resena/**"  // corregí para que tenga la barra inicial
                        ).hasAnyRole("CLIENTE","ADMIN")
                        .requestMatchers(
                                "/api/carta/**",
                                "/api/carta/*",
                                "/api/restaurante/actualizar/*"
                        ).hasAnyRole("ENCARGADO", "ADMIN")
                        .requestMatchers("/api/admin/*/aprobar").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public JwtAuthFilter jwtAuthFilter() {
        return new JwtAuthFilter();
    }




}