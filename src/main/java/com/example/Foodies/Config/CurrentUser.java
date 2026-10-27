package com.example.Foodies.Config;

import com.example.Foodies.Exception.ForbiddenException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** The JWT subject is the user's email; {@link JwtAuthFilter} stores it as the authentication name. */
@Component
public class CurrentUser {

    public String email() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getName() == null) {
            throw new ForbiddenException("No hay un usuario autenticado");
        }
        return auth.getName();
    }
}
