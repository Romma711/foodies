package com.example.Foodies.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain (HttpSecurity http) throws Exception{
        http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/restaurants").permitAll()
                        .requestMatchers("/api/reseñas").permitAll()
                        .requestMatchers("/api/cliente/create").permitAll()
                        .requestMatchers("/api/reserva/create").hasRole("CLIENTE")
                        .requestMatchers("/api/cliente/update").hasRole("CLIENTE")
                        .requestMatchers("/api/carta/*/descargar").permitAll()
                        .requestMatchers("/api/carta/**").hasAnyRole("ENCARGADO","ADMIN")
                        .requestMatchers("/api/admin/restaurantes/*/aprobar").hasRole("ADMIN")
                        .requestMatchers("/api/registro/restaurante").permitAll()
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .httpBasic(basic -> {}
                );
        return http.build();
    }
}
