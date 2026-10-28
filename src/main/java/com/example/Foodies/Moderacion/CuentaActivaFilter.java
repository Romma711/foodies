package com.example.Foodies.Moderacion;

import com.example.Foodies.Usuario.Usuario;
import com.example.Foodies.Usuario.UsuarioRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

/**
 * Aplica la baja lógica de usuarios (Fase 3) sin modificar el login ni el filtro JWT existentes.
 *
 * <ul>
 *   <li>{@code POST /api/auth/login}: si el email y la contraseña son correctos pero la cuenta está
 *       dada de baja, responde 403. Con credenciales incorrectas no dice nada de la baja: el login
 *       responde como siempre, así no se revela qué cuentas fueron dadas de baja.</li>
 *   <li>Cualquier otra request autenticada: si la cuenta del token fue dada de baja después de
 *       emitirlo, responde 403.</li>
 * </ul>
 *
 * <p>Se registra como filtro de servlet común y corre después de la cadena de Spring Security, por
 * eso ya encuentra el usuario autenticado en el {@link SecurityContextHolder}.</p>
 */
@Component
public class CuentaActivaFilter extends OncePerRequestFilter {

    private static final String LOGIN = "/api/auth/login";

    private final UsuarioRepository usuarioRepo;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    public CuentaActivaFilter(UsuarioRepository usuarioRepo, PasswordEncoder passwordEncoder, ObjectMapper objectMapper) {
        this.usuarioRepo = usuarioRepo;
        this.passwordEncoder = passwordEncoder;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (HttpMethod.POST.matches(request.getMethod()) && LOGIN.equals(request.getServletPath())) {
            // The login controller still needs to read the body, so it is buffered once.
            CuerpoReleible releible = new CuerpoReleible(request);
            if (loginDeCuentaDadaDeBaja(releible.cuerpo())) {
                rechazar(response);
                return;
            }
            chain.doFilter(releible, response);
            return;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)
                && usuarioRepo.findByEmail(auth.getName()).map(this::dadaDeBaja).orElse(false)) {
            rechazar(response);
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean loginDeCuentaDadaDeBaja(byte[] cuerpo) {
        try {
            JsonNode json = objectMapper.readTree(cuerpo);
            String email = json.path("email").asText(null);
            String password = json.path("password").asText(null);
            if (email == null || password == null) {
                return false;
            }
            Optional<Usuario> usuario = usuarioRepo.findByEmail(email);
            return usuario.isPresent()
                    && dadaDeBaja(usuario.get())
                    && passwordEncoder.matches(password, usuario.get().getPassword());
        } catch (IOException ex) {
            // Malformed body: let the login endpoint answer as it always did.
            return false;
        }
    }

    private boolean dadaDeBaja(Usuario usuario) {
        return Boolean.FALSE.equals(usuario.getActivo());
    }

    private void rechazar(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), Map.of(
                "error", "cuenta dada de baja",
                "mensaje", "Tu cuenta fue dada de baja. Escribinos si creés que es un error."));
    }
}
