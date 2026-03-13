package com.ing.assesment.domain.auth.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
public class User {

    private Long id;
    private String email;
    private String passwordHash;
    private Set<UserRole> roles = new HashSet<>();
    private Instant createdAt;
    private Instant lastLoginAt;

    private User(String email, String passwordHash) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.roles.add(UserRole.CUSTOMER);
        this.createdAt = Instant.now();
    }

    public static User create(String email, String passwordHash) {
        return new User(email, passwordHash);
    }

    public void markLoggedIn(Instant loginTime) {
        this.lastLoginAt = loginTime;
    }
}
