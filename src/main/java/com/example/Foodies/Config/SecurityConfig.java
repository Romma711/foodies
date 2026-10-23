package com.example.Foodies.Config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
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
                // Rules are evaluated in order: the first matching rule wins, so specific paths go first.
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(HttpMethod.POST,
                                "/api/auth/login",
                                "/api/auth/register/cliente",
                                "/api/auth/register/restaurante"
                        ).permitAll()
                        // Restaurant manager panel: always scoped to the caller's own restaurant.
                        .requestMatchers("/api/restaurantes/me", "/api/restaurantes/me/**").hasAuthority("ROLE_ENCARGADO")
                        .requestMatchers(HttpMethod.PATCH, "/api/reservas/*/estado").hasAuthority("ROLE_ENCARGADO")
                        // Public catalog: read-only.
                        .requestMatchers(HttpMethod.GET,
                                "/api/restaurantes",
                                "/api/restaurantes/especialidad",
                                "/api/restaurantes/*",
                                "/api/restaurantes/*/carta",
                                "/api/carta/*",
                                "/api/files/**"
                        ).permitAll()
                        // Writes by id are not ownership-checked, so only admins may use them.
                        .requestMatchers("/api/restaurantes/*", "/api/carta/**", "/api/reservas/restaurante/*").hasAuthority("ROLE_ADMIN")
                        .requestMatchers(
                                "/api/reservas/**",
                                "/api/clientes/*",
                                "/api/resenas/**"
                        ).hasAnyAuthority("ROLE_CLIENTE","ROLE_ADMIN")
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