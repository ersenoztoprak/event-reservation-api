package com.ing.assesment.infra.auth.security;

import com.ing.assesment.domain.auth.model.UserRole;

import java.util.Set;

public record SecurityUser(
        Long id,
        String email,
        Set<UserRole> roles) {
}
