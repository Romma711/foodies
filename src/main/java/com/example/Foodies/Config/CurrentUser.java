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

    /** True when the authenticated user has {@code ROLE_ADMIN}. */
    public boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }
}
