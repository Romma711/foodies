package com.example.Foodies.Config;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.util.Date;
import java.util.List;

public class JwtUtil {

    private static final String SECRET = System.getenv("JWT_SECRET") != null
            ? System.getenv("JWT_SECRET")
            : "clave-ultra-secreta-andy-deja-el-lol";


    private static final long EXPIRATION = 3600000L; // 1 hora


    public static String createToken(String username, List<String> roles) {
        return Jwts.builder()
                .setSubject(username)                   // claim "sub"
                .claim("roles", roles)                // claim personalizado
                .setIssuedAt(new Date())                // claim "iat"
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION)) // "exp"
                .signWith(SignatureAlgorithm.HS256, SECRET) // firma con HMAC+SHA256
                .compact();                             // construye el string
    }


    public static boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .setSigningKey(SECRET)
                    .parseClaimsJws(token);  // lanza excepción si inválido o expirado
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
                .setSigningKey(SECRET)
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }


    @SuppressWarnings("unchecked")
    public static List<String> getRoles(String token) {
        return (List<String>) Jwts.parser()
                .setSigningKey(SECRET)
                .parseClaimsJws(token)
                .getBody()
                .get("roles");
    }
}
