package com.ing.assesment.domain.auth.model;

import java.util.Set;

public record AuthenticatedUser(
        Long id,
        String email,
        Set<UserRole> roles) {

    public boolean isAdmin() {
        return roles.contains(UserRole.ADMIN);
    }

    public boolean isOrganizer() {
        return roles.contains(UserRole.ORGANIZER);
    }
}
