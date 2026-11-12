package com.example.Foodies.Config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

/**
 * Creates and reads the JWTs. The signing secret comes from the {@code jwt.secret} property
 * (environment variable {@code JWT_SECRET}); there is no fallback, so the app refuses to start
 * without a secret of at least 32 bytes (256 bits, the minimum for HS256).
 */
@Component
public class JwtUtil {

    static final int MIN_SECRET_BYTES = 32;

    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

    private static final long EXPIRATION = 18000000L; // 5 horas |  3600000L 1 hora

    private final SecretKey key;

    public JwtUtil(@Value("${jwt.secret:}") String secret) {
        this.key = Keys.hmacShaKeyFor(validSecret(secret));
    }

    static byte[] validSecret(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "Falta la clave JWT: definí la variable de entorno JWT_SECRET (al menos 32 bytes).");
        }
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "La clave JWT es demasiado corta: JWT_SECRET debe tener al menos 32 bytes (tiene "
                            + bytes.length + ").");
        }
        return bytes;
    }

    public String createToken(String username, List<String> roles, Long usuarioId) {
        return Jwts.builder()
                .setSubject(username)
                .claim("roles", roles)
                .claim("usuarioId", usuarioId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .signWith(key)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            claims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.debug("JWT expirado: {}", e.getMessage());
        } catch (Exception e) {
            log.debug("JWT inválido: {}", e.getMessage());
        }
        return false;
    }

    public String getUsername(String token) {
        return claims(token).getSubject();
    }

    @SuppressWarnings("unchecked")
    public List<String> getRoles(String token) {
        return (List<String>) claims(token).get("roles");
    }

    private Claims claims(String token) {
        return Jwts.parser()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
