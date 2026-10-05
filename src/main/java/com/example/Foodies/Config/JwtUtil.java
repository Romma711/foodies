package com.example.Foodies.Config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class JwtUtil {
    /**
     * La clave de firma NUNCA se hardcodea: viene del entorno.
     * Si falta, la app no arranca (ver JwtSecretConfig).
     */
    private static final String SECRET = cargarSecret();

    private static final long EXPIRATION =18000000L; // 5 horas |  3600000L 1 hora

    private static final SecretKey KEY = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

    private static String cargarSecret() {
        String secret = System.getProperty("JWT_SECRET");
        if (secret == null || secret.isBlank()) {
            secret = System.getenv("JWT_SECRET");
        }
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "Falta la variable de entorno JWT_SECRET: definala antes de arrancar la aplicacion "
                            + "(ej: JWT_SECRET=<clave-de-32-bytes-o-mas> ./mvnw spring-boot:run)");
        }
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT_SECRET debe tener al menos 32 bytes (256 bits)");
        }
        return secret;
    }

    /** Llamado al arrancar para fallar rapido si la clave no esta configurada. */
    public static void verificarConfiguracion() {
        if (KEY == null || SECRET == null || SECRET.isBlank()) {
            throw new IllegalStateException("JWT_SECRET no esta configurado");
        }
    }


    public static String createToken(String username, List<String> roles) {
        return Jwts.builder()
                .setSubject(username)
                .claim("roles", roles)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .signWith(KEY)
                .compact();
    }

    public static boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .setSigningKey(KEY)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException e) {
            System.err.println("JWT expirado: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("JWT inválido: " + e.getMessage());
        }
        return false;
    }

    public static String getUsername(String token) {
        return Jwts.parser()
                .setSigningKey(KEY)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    @SuppressWarnings("unchecked")
    public static List<String> getRoles(String token) {
        return (List<String>) Jwts.parser()
                .setSigningKey(KEY)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get("roles");
    }

    public static void printTokenInfo(String token) {
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(KEY)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            System.out.println("👉 Información del token:");
            for (Map.Entry<String, Object> entry : claims.entrySet()) {
                System.out.println("  🔹 " + entry.getKey() + ": " + entry.getValue());
            }

        } catch (Exception e) {
            System.err.println("❌ Token inválido: " + e.getMessage());
        }
    }
}