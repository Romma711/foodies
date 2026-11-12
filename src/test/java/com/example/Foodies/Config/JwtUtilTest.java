package com.example.Foodies.Config;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtUtilTest {

    @Test
    void refusesToStartWithoutASecret() {
        assertThatThrownBy(() -> new JwtUtil(""))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");
        assertThatThrownBy(() -> new JwtUtil(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");
    }

    @Test
    void refusesASecretShorterThan32Bytes() {
        assertThatThrownBy(() -> new JwtUtil("a".repeat(31)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("al menos 32 bytes");
    }

    @Test
    void aValidSecretSignsAndReadsTokens() {
        JwtUtil jwt = new JwtUtil("a".repeat(32));

        String token = jwt.createToken("ana@test.com", List.of("ROLE_CLIENTE"), 7L);

        assertThat(jwt.validateToken(token)).isTrue();
        assertThat(jwt.getUsername(token)).isEqualTo("ana@test.com");
        assertThat(jwt.getRoles(token)).containsExactly("ROLE_CLIENTE");
    }

    @Test
    void aTokenSignedWithAnotherSecretIsRejected() {
        String token = new JwtUtil("a".repeat(32)).createToken("ana@test.com", List.of("ROLE_CLIENTE"), 7L);

        assertThat(new JwtUtil("b".repeat(32)).validateToken(token)).isFalse();
    }
}
