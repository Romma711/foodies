package com.example.Foodies;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * A clock the tests can move: reservation rules read "now" from the {@link Clock} bean, so a test can
 * book for tomorrow and then jump to the arrival time. Import {@link Config} to replace the real clock.
 */
public class RelojDePrueba extends Clock {

    private final ZoneId zona = ZoneId.systemDefault();
    private volatile Instant instante = Instant.now();

    /** Back to the real current time. */
    public void reiniciar() {
        instante = Instant.now();
    }

    public void fijar(LocalDateTime ahora) {
        instante = ahora.atZone(zona).toInstant();
    }

    @Override
    public ZoneId getZone() {
        return zona;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }

    @Override
    public Instant instant() {
        return instante;
    }

    @TestConfiguration
    public static class Config {
        @Bean
        @Primary
        public RelojDePrueba relojDePrueba() {
            return new RelojDePrueba();
        }
    }
}
