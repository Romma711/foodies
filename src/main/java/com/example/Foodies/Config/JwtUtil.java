package com.example.Foodies.Config;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

public class JwtUtil {
    private static final String SECRET = System.getenv("JWT_SECRET") != null
            ? System.getenv("JWT_SECRET")
            : "clave-ultra-secreta-andy-deja-el-lol-necesitamos-una-clave-mas-larga-para-cumplir-con-256-bits";

    private static final long EXPIRATION = 3600000L; // 1 hora

    private static final SecretKey KEY = Jwts.SIG.HS256.key().build();


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
}