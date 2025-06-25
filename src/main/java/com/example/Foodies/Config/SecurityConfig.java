package com.example.Foodies.Config;

import com.example.Foodies.Enums.Role;
import com.example.Foodies.Usuario.Usuario;
import com.example.Foodies.Usuario.UsuarioRepository;
import jakarta.servlet.http.HttpServletResponse;
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
                .exceptionHandling(ex -> ex
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"error\": \"No tenés permisos para acceder a este recurso\"}");
                        })
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"error\": \"No estás autenticado\"}");
                        })
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/carta/*/descargar",
                                "/api/auth/login",
                                "/api/auth/register/cliente",
                                "/api/auth/register/restaurant",
                                "/api/restaurante/all",
                                "/api/restaurante/especialidad",
                                "/api/restaurante/{id}",
                                "/api/auth/test",
                                "/error"  // <-- Agregado para permitir acceso a la página de error
                        ).permitAll()
                        .requestMatchers(
                                "/api/reserva/**",
                                "/api/cliente/*",
                                "/api/resena/**"  // corregí para que tenga la barra inicial
                        ).hasAnyAuthority("ROLE_CLIENTE","ROLE_ADMIN")
                        .requestMatchers(
                                "/api/carta/**",
                                "/api/reserva/list_by_restaurant/*",
                                "/api/carta/*",
                                "/api/reserva/*",
                                "/api/restaurante/actualizar/*",
                                "/api/restaurante/*/delete"
                        ).hasAnyAuthority("ROLE_ENCARGADO", "ROLE_ADMIN")
                        .requestMatchers("/api/admin/**").hasAuthority("ROLE_ADMIN")
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