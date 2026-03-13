package com.ing.assesment.infra.auth.security;

import com.ing.assesment.domain.auth.model.AuthenticatedUser;
import com.ing.assesment.domain.auth.model.UserRole;
import com.ing.assesment.domain.auth.port.CurrentUserPort;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Set;

public class CurrentUserAdapter implements CurrentUserPort {

    @Override
    public AuthenticatedUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() ||
                authentication instanceof AnonymousAuthenticationToken) {
            throw new org.springframework.security.access.AccessDeniedException("Unauthenticated");
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof SecurityUser(Long id, String email, Set<UserRole> roles))) {
            throw new org.springframework.security.access.AccessDeniedException("Invalid authenticated principal");
        }

        return new AuthenticatedUser(
                id,
                email,
                roles
        );
    }
}
