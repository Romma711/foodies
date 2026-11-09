package com.example.Foodies.Exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    @Test
    void unErrorInesperadoResponde500SinDetallesInternos() {
        ResponseEntity<?> resp = new GlobalExceptionHandler()
                .handleUnexpected(new NullPointerException("Cannot invoke \"Usuario.getId()\" because x is null"));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        Map<?, ?> body = (Map<?, ?>) resp.getBody();
        assertThat(body.get("error")).isEqualTo("error interno");
        assertThat(body.containsKey("trace")).isFalse();
        assertThat(body.toString()).doesNotContain("Usuario.getId").doesNotContain("NullPointer");
    }
}
